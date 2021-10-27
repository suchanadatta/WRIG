import numpy as np
import matplotlib.pyplot as plt
import sys

if len(sys.argv) < 13:
    print('Needs 12 argument...')
    exit(0)

wrig_lr_w2v = sys.argv[1]
wrig_lr_rlm = sys.argv[2]
wrig_lr_uqv = sys.argv[3]
wrig_sigma_w2v = sys.argv[4]
wrig_sigma_rlm = sys.argv[5]
wrig_sigma_uqv = sys.argv[6]
# jm_lr_w2v = sys.argv[7]
# jm_lr_rlm = sys.argv[8]
# jm_lr_uqv = sys.argv[9]
# jm_sigma_w2v = sys.argv[10]
# jm_sigma_rlm = sys.argv[11]
# jm_sigma_uqv = sys.argv[12]
actual_ap = sys.argv[10]

read_wrig_lr_w2v = np.genfromtxt(wrig_lr_w2v, dtype=np.float64, delimiter='\t', skip_header=0)
read_wrig_lr_rlm = np.genfromtxt(wrig_lr_rlm, dtype=np.float64, delimiter='\t', skip_header=0)
read_wrig_lr_uqv = np.genfromtxt(wrig_lr_uqv, dtype=np.float64, delimiter='\t', skip_header=0)
read_wrig_sigma_w2v = np.genfromtxt(wrig_sigma_w2v, dtype=np.float64, delimiter='\t', skip_header=0)
read_wrig_sigma_rlm = np.genfromtxt(wrig_sigma_rlm, dtype=np.float64, delimiter='\t', skip_header=0)
read_wrig_sigma_uqv = np.genfromtxt(wrig_sigma_uqv, dtype=np.float64, delimiter='\t', skip_header=0)
# read_jm_lr_w2v = np.genfromtxt(jm_lr_w2v, dtype=np.float64, delimiter='\t', skip_header=0)
# read_jm_lr_rlm = np.genfromtxt(jm_lr_rlm, dtype=np.float64, delimiter='\t', skip_header=0)
# read_jm_lr_uqv = np.genfromtxt(jm_lr_uqv, dtype=np.float64, delimiter='\t', skip_header=0)
# read_jm_sigma_w2v = np.genfromtxt(jm_sigma_w2v, dtype=np.float64, delimiter='\t', skip_header=0)
# read_jm_sigma_rlm = np.genfromtxt(jm_sigma_rlm, dtype=np.float64, delimiter='\t', skip_header=0)
# read_jm_sigma_uqv = np.genfromtxt(jm_sigma_uqv, dtype=np.float64, delimiter='\t', skip_header=0)
read_actual_ap = np.genfromtxt(actual_ap, dtype=np.float64, delimiter='\t', skip_header=0)


# SMALL_SIZE = 10
# MEDIUM_SIZE = 13
# BIGGER_SIZE = 28
#
# plt.rc('font', size=SMALL_SIZE)          # controls default text sizes
# plt.rc('axes', titlesize=SMALL_SIZE)     # fontsize of the axes title
# plt.rc('axes', labelsize=MEDIUM_SIZE)    # fontsize of the x and y labels
# plt.rc('xtick', labelsize=MEDIUM_SIZE)    # fontsize of the tick labels
# plt.rc('ytick', labelsize=MEDIUM_SIZE)    # fontsize of the tick labels
# plt.rc('legend', fontsize=SMALL_SIZE)    # legend fontsize
# plt.rc('figure', titlesize=BIGGER_SIZE)  # fontsize of the figure title

read_files = [read_wrig_lr_w2v, read_wrig_lr_rlm, read_wrig_lr_uqv,
              read_wrig_sigma_w2v, read_wrig_sigma_rlm, read_wrig_sigma_uqv]
              # read_jm_lr_w2v, read_jm_lr_rlm, read_jm_lr_uqv,
              # read_jm_sigma_w2v, read_jm_sigma_rlm, read_jm_sigma_uqv]

fig, axes = plt.subplots(nrows=2, ncols=3, gridspec_kw={'width_ratios': [1, 1, 1], 'height_ratios': [0.6, 0.6]})
xticks = np.linspace(0, 1, 11, endpoint=True)
yticks = np.linspace(0, 1, 11, endpoint=True)
fig_list = ['(a)', '(b)', '(c)', '(d)', '(e)', '(f)']
scatter_color = ['blue', 'red', 'green', 'blue', 'red', 'green']
# , 'orange', 'black', 'yellow', 'orange', 'black', 'yellow']
marker = ['o', 'x', '^', 'o', 'x', '^']
    # , 'o', 'x', '^', 'o', 'x', '^']

d,e = 0,0
for c, file in enumerate(read_files):
    # if np.ndim(file) == 1:
    #     read_files[c] = file[np.newaxis]
    # if c >= len(read_files)-1:
    #     break
    if e == 3:
        d += 1
        e = 0
    x = read_actual_ap[:,1]
    print(x[0:10])
    y = file[:,1]
    print(y[0:10] , '\n======================\n')
    axes[d, e].scatter(x, y, c=scatter_color[int(c)], marker=marker[int(c)], s=20)
    axes[d, e].set_xticks(xticks)
    axes[d, e].set_yticks(yticks)

    axes[d, e].set_xlabel("Actual_AP", fontsize=18)
    axes[d, e].set_ylabel("Predicted_AP", fontsize=18)
    axes[d, e].set_title(fig_list[c], fontsize=18)
    e += 1

fig.suptitle("Per query analysis", fontsize=24, fontweight='bold')
# plt.subplots_adjust(left=0.045, right=0.965, bottom=0.55, top=0.87)
plt.show()












