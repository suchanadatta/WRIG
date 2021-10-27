def RBO_score(list1, list2, p):
    """
        Calculates Ranked Biased Overlap (RBO) score.
        l1 -- Ranked List 1
        l2 -- Ranked List 2
    """
    if list1 == 0 or list2 == 0: return 0
    short_list, long_list = sorted([(len(list1), list1), (len(list2), list2)])
    s, S = short_list
    l, L = long_list

    # Calculate the overlaps at ranks 1 through l
    # (the longer of the two lists)
    ss = set([])
    ls = set([])
    overs = {}
    for i in range(l):
        ls.add(L[i])
        if i < s:
            ss.add(S[i])
        X_d = len(ss.intersection(ls))
        d = i + 1
        overs[d] = float(X_d)

    # (1) \sum_{d=1}^l (X_d / d) * p^d
    sum1 = 0
    for i in range(l):
        d = i + 1
        sum1 += overs[d] / d * pow(p, d)
    # X_s = overs[s]
    # X_l = overs[l]
    #
    # # (2) \sum_{d=s+1}^l [(X_s (d - s)) / (sd)] * p^d
    # sum2 = 0
    # for i in range(s, l):
    #     d = i + 1
    #     sum2 += (X_s * (d - s) / (s * d)) * pow(p, d)
    #
    # # (3) [(X_l - X_s) / l + X_s / s] * p^l
    # sum3 = ((X_l - X_s) / l + X_s / s) * pow(p, l)
    #
    # # Equation 32.
    # rbo_ext = (1 - p) / p * (sum1 + sum2) + sum3
    return sum1

# list1 = ['0', '1', '2', '3', '4', '5']
# list2 = ['1', '0', '2', '3', '4', '5']
# print(RBO_score(list1, list2, 0.9))

list1 = ['A','B','C','D','E','H']
list2 = ['D','B','F','A']
print(RBO_score(list1, list2, 0.9))

# ========================= (https://towardsdatascience.com/rbo-v-s-kendall-tau-to-compare-ranked-lists-of-items-8776c5182899)

import math
def rbo(list1, list2, p=0.9):
   # tail recursive helper function
   def helper(ret, i, d):
       l1 = set(list1[:i]) if i < len(list1) else set(list1)
       l2 = set(list2[:i]) if i < len(list2) else set(list2)
       a_d = len(l1.intersection(l2))/i
       term = math.pow(p, i) * a_d
       if d == i:
           return ret + term
       return helper(ret + term, i + 1, d)
   k = max(len(list1), len(list2))
   x_k = len(set(list1).intersection(set(list2)))
   summation = helper(0, 1, k)
   return ((float(x_k)/k) * math.pow(p, k)) + ((1-p)/p * summation)

# Example usage
rbo([1,2,3], [3,2,1]) # Output: 0.8550000000000001


