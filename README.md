# WRIG
A Relative Information Gain based Query Performance Prediction Framework with Automatically Generated Query Variants

﻿# DeepQPP
This is a pairwise interaction based Deep Learning Model for supervised query performance prediction. 
The entire model is comprised of two modules - 1. **InteractionMatrix** (developed with Java) and 
2. **DeepQPP** (written in Python) 

## Requirements
To run the DeepQPP model, just check if your conda environment is good with the following python packages. 
In case you want to use any higher or lower version of any of the followings, check if there is any 
compatibility issue.
````````````````````````````````````````````
# for InteractionMatrix (java module)
jdk 1.8.0 or above
lucene 5.3.1

# for DeepQPP learning (python module)
conda 4.8.2
python 3.7.9
numpy 1.19.4
keras 2.3.0
tensorflow 2.2.0
scikit-learn 0.23.2

# for SN-BERT
nltk 3.5
transformers 4.6.1
````````````````````````````````````````````
## Guide to use
**Step-1:** Download TREC-Robust [java index and word vectors](https://drive.google.com/drive/folders/13k0AFcIemmtBvBpaBCyJR7ZYUIoRf2Kx?usp=sharing) here.

**Step-2:** Create a [conda environment](https://phoenixnap.com/kb/how-to-install-anaconda-ubuntu-18-04-or-20-04) 
and activate it using the command - 
> conda activate <environment_name>

**Step-3:** Check all the packages listed above using correct version of your pip -
> pip list

In case required packages are missing, install the right version in your current conda environment by running -
> pip install -r requirements.txt 

There is a top level bash script **main.sh**. Firstly, it runs the InteractionMatrix module to generate matching histograms of pseudo-relevant documents as proposed in the paper : [A Deep Relevance Matching Model for Ad-hoc Retrieval](https://dl.acm.org/doi/10.1145/2983323.2983769). It computes Log-IDF based histograms for a document with respect to a given query. This is built on top of LCH(Log-Count-based Histogram); LCH(with IDF) performs the best as reported in the paper. Given a query, interaction matrices computed for the set of respective relevant documents are stored in a single file with the name **query_id.hist**.

**Step-4:** Provide all arguments in order to run the bash script **interaction.sh** in main.sh. Following arguments should be given :
``````````````````````````````````````````````````````````````````````````````````````````
> Query file (in .xml format)
> Path of the lucene index
> Stopwords file
> SimilarityFunction (DefaultSimilarity / BM25Similarity / LMJelinekMercerSimilarity / LMDirichletSimilarity)
> No. of top documents to retrieve
> Path of the directory to store initial retrieved documents
> Word vector file path
> Name of the field used for searching (default 'content'- if using available index with this project)
> Interaction matrix path
``````````````````````````````````````````````````````````````````````````````````````````

Next, supervised deepQPP module is trained by a set of query pairs' relative specificity computed through **query_pair_judgement.py**. We train the model with paired data and tested with both paired and point test set. K-fold cross validation is used to test model's efficiency. 

**Step-5:** Following arguments to be given in order to run the bash script **qppeval.sh** through main.sh. Check if arguments below are set in main.sh -
``````````````````````````````````````````````````````````````````````````````````````````
> Path of the AP file
> Path of the interaction matrix (separate file for each qid)
> Training batch size
> No. of epochs
> No. of cross validation folds
> Type of evaluation : pair/point
> Want to save predicted values? -- yes/no
``````````````````````````````````````````````````````````````````````````````````````````
**Step-6:** Run the top level script
> sh main.sh

