import sys
import numpy as np
from statistics import mean
import pandas as pd
from scipy import stats

if len(sys.argv) < 8:
    print('Needs 7 arguments - \n1. TREC initial query based LM reranked file (reranked by DRMM)\n'
          '2. UQV-TREC query variants based LM reranked file (reranked by DRMM)\n'
          '3. TREC initial query .AP file\n'
          '4. res file path\n'
          '5. No. of top documents retrieved by initial query in the initial.res file\n'
          '6. No. of top documents retrieved by query variant in the variant.res file\n'
          '7. No. of top scores to be considered for each variant')
    exit(0)

arg_trec_prerank_file = sys.argv[1]
arg_uqv_prerank_file = sys.argv[2]
arg_trec_ap_file = sys.argv[3]
arg_res_path = sys.argv[4]
arg_top_docs_iq = int(sys.argv[5])
arg_top_docs_vq = int(sys.argv[6])
arg_top_scores_vq = int(sys.argv[7])

# fw_regression = open(arg_res_path + 'TD-' + str(arg_top_docs_iq) + 'TS-' + str(arg_top_docs_vq) + '_variance.res', 'w')

trec_r_Q = {}
uqv_r_Q = {}
nqc_r_dict = {}


def compute_regression_trec(score_list, qid):
    doc_rank = np.arange(1, len(score_list) + 1, 1)
    # print('doc rank :', doc_rank)
    score_list = np.array(score_list).astype(np.float)
    # print('score list : ', score_list)
    # print('shape of score list : ', score_list.shape)
    slop = (((mean(doc_rank) * mean(score_list)) - mean(doc_rank * score_list)) /
         ((mean(doc_rank) * mean(doc_rank)) - mean(doc_rank * doc_rank)))
    # print('slop : ', slop)
    intercept = mean(score_list) - slop * mean(doc_rank)
    # print('intercept : ', intercept)
    trec_r_Q[qid] = round(abs(slop), 4)


def linear_regression_trec_query(trec_res_file, topdocs):
    qid = ""
    count = 0
    per_query_score_list = []
    fp = open(trec_res_file)
    for line in fp.readlines():
        parts = line.split('\t')
        if qid == "" or parts[0] == qid:
            if count < topdocs:
                qid = parts[0]
                score = parts[4]
                per_query_score_list.append(score)
                count = count + 1
        elif parts[0] != qid:
            # print('query : ', qid, '\t', per_query_score_list)
            compute_regression_trec(per_query_score_list, qid)
            per_query_score_list = []
            count = 0
            qid = parts[0]
            score = parts[4]
            per_query_score_list.append(score)
            count = count + 1
    # print('query : ', qid, '\t', per_query_score_list)
    compute_regression_trec(per_query_score_list, qid)


def compute_regression_uqv(score_list, varscores):
    # print("sorted list : ", score_list)
    score_list = np.array(score_list).astype(np.float)
    score_list = score_list[0:varscores]
    # print('sub-score list : ', score_list)
    doc_rank = np.arange(1, len(score_list) + 1, 1)
    # print('doc rank :', doc_rank)
    slop = (((mean(doc_rank) * mean(score_list)) - mean(doc_rank * score_list)) /
            ((mean(doc_rank) * mean(doc_rank)) - mean(doc_rank * doc_rank)))
    # print('slop : ', slop)
    intercept = mean(score_list) - slop * mean(doc_rank)
    # print('intercept : ', intercept)
    return round(abs(slop), 6)


def linear_regression_uqv_query(uqv_res_file, topdocs, varscores):
    qid = ""
    count = 0
    fp = open(uqv_res_file)
    per_query_score_list = []
    per_query_QV_list = []
    for line in fp.readlines():
        parts = line.split('\t')
        if qid == "" or parts[0] == qid:
            if count < topdocs:
                per_query_score_list.append(parts[4])
                qid = parts[0]
                count += 1
            else:
                # print('query : ', qid, '\t', 'score list size : ', len(per_query_score_list))
                per_query_score_list = sorted(per_query_score_list, reverse=True)
                regression = compute_regression_uqv(per_query_score_list, varscores)
                per_query_QV_list.append(regression)
                count = 1
                qid = parts[0]
                per_query_score_list = [parts[4]]
        elif parts[0] != qid:
            # print('query : ', qid, '\t', 'score list size : ', len(per_query_score_list))
            per_query_score_list = sorted(per_query_score_list, reverse=True)
            regression = compute_regression_uqv(per_query_score_list, varscores)
            per_query_QV_list.append(regression)
            uqv_r_Q[qid] = per_query_QV_list
            # print('query : ', qid, '\t', 'no. of query variants : ', len(per_query_QV_list))
            count = 1
            qid = parts[0]
            per_query_score_list = [parts[4]]
            per_query_QV_list = []
    # print('query : ', qid, '\t', 'score list size : ', len(per_query_score_list))
    per_query_score_list = sorted(per_query_score_list, reverse=True)
    regression = compute_regression_uqv(per_query_score_list, varscores)
    per_query_QV_list.append(regression)
    uqv_r_Q[qid] = per_query_QV_list
    # print('query : ', qid, '\t', 'no. of query variants : ', len(per_query_QV_list))
    # print("final list : ", uqv_r_Q)


linear_regression_trec_query(arg_trec_prerank_file, arg_top_docs_iq)
print("\nTREC res file variance dict : ", trec_r_Q)
linear_regression_uqv_query(arg_uqv_prerank_file, arg_top_docs_vq, arg_top_scores_vq)
print("\nUQV res file variance dict : ", uqv_r_Q)

# del_v(Q,Q') = X(v(Q')) - v(Q) / v(Q) : X = {max, min, avg}
for key in trec_r_Q:
    # print("key : ", key)
    q_dash = uqv_r_Q[key]
    # print("qdash : ", q_dash)
    # max_v_Q_var = max(q_dash)
    # print("max(v(Q')) : ", max_v_Q_var)
    # min_v_Q_var = min(q_dash)
    # print("min(v(Q')) : ", min_v_Q_var)
    avg_v_Q_var = sum(q_dash) / len(q_dash)
    # print("avg(v(Q')) : ", avg_v_Q_var)
    # max_v_Q_var = (trec_v_Q[key] - max_v_Q_var) / trec_v_Q[key]
    # min_v_Q_var = trec_v_Q[key] - min_v_Q_var / trec_v_Q[key]
    avg_v_Q_var = (trec_r_Q[key] - avg_v_Q_var) / trec_r_Q[key]
    # nqc_r_dict[key] = round(max_v_Q_var, 4)
    # nqc_v_dict[key] = round(min_v_Q_var, 4)
    nqc_r_dict[key] = round(avg_v_Q_var, 4)
print("\ndel_v_(Q,Q') : ", nqc_r_dict)

fp = open(arg_trec_ap_file)
ap_scores = []
nqc_regression_scores = []
for line in fp.readlines():
    ap_scores.append(float(line.split('\t')[1]))
for key in nqc_r_dict:
    nqc_regression_scores.append(nqc_r_dict[key])

xranks = pd.Series(ap_scores).rank()
# print("Rankings of X:", xranks)
yranks = pd.Series(nqc_regression_scores).rank()
# print("Rankings of Y:", yranks)
rho, _ = stats.spearmanr(ap_scores, nqc_regression_scores)
# print("\nSpearman's Rank correlation:", round(rho, 4))

tau, _ = stats.kendalltau(ap_scores, nqc_regression_scores)
# print('Kendall Rank correlation: %.5f' % tau)

print(round(rho, 4), '\t', round(tau, 4))
