import sys
from keras.callbacks import ModelCheckpoint
from keras.utils import plot_model
from keras_model import build_keras_model
from load_data import *
from loss_function import *
import numpy as np
import os

# make sure the argument is good (0 = the python file, 1+ the actual argument)

if len(sys.argv) < 7:
    print('Needs 5 arguments-\n1. run name\n'
                              '2. train pair file\n'
                              '3. train histogram file\n'
                              '4. test file\n'
                              '5. test histogram file\n'
                              '6. reranked file path.')
    exit(0)

run_name = sys.argv[1]
train_file = sys.argv[2]
train_file_histogram = sys.argv[3]
test_file = sys.argv[4]
test_file_histogram = sys.argv[5]
reranked_file_path = sys.argv[6]

#
# build and train model
#
model = build_keras_model()
model.summary()
model.compile(loss=rank_hinge_loss, optimizer='adam')  # adam
# plot_model(model, to_file='model.png', show_shapes=True)

train_input, train_labels = get_keras_train_input(train_file, train_file_histogram)
if not os.path.exists(reranked_file_path):
    os.makedirs(reranked_file_path)
# c1 = ModelCheckpoint(filepath='models/temp_'+run_name+'.hdf5', verbose=1, save_best_only=False)
model.fit(train_input, train_labels, batch_size=10, verbose=2, shuffle=False, epochs=100)  # , callbacks=[c1])
model.save_weights(reranked_file_path + run_name + '.weights')

#
# prediction
#

test_data, pre_rank_data = get_keras_test_input(test_file, test_file_histogram)
predictions = model.predict(test_data, batch_size=10)
if not os.path.exists(reranked_file_path):
    os.makedirs(reranked_file_path)
with open(reranked_file_path + run_name + '.result', 'w') as outFile:
    print("========== res file name : ", run_name, '===========')
    i = 0
    for topic, doc in pre_rank_data:
        outFile.write(topic + '\t' + 'Q0' + '\t' + doc + "\t" + str(predictions[i][0]) + '\t' + 'drmmm' + '\n')
        i += 1
