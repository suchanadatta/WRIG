# WRIG
WRIG is a Relative Information Gain based Query Performance Prediction Framework with Automatically Generated Query Variants.
The entire project is comprised of a number of sub-modules -
1. **InteractionMatrix** (developed with Java) 
2. **RerankByDRMM** (written in Python) 
3. **QPPEval** (in Java)
4. **GenerateQueryVariants** (in Java) and
5. **WRIG** (in Python)

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
````````````````````````````````````````````
## Guide to use
**Step-1:** Download TREC-Robust [java index and word vectors](https://drive.google.com/drive/folders/13k0AFcIemmtBvBpaBCyJR7ZYUIoRf2Kx?usp=sharing) here.

**Step-2:** Create a [conda environment](https://phoenixnap.com/kb/how-to-install-anaconda-ubuntu-18-04-or-20-04) 
and activate it using the command - 
> conda activate <environment_name>

**Step-3:** Check all the packages listed above using correct version of your pip -
> pip list

In case required packages are missing, install the right version in your current conda environment by running -
> pip install <package-name>

There is a top level bash script **wrig.sh**. 
