# 'Learning to Rank' (LETOR) Text based Search Engine 
[![Build Status](https://travis-ci.com/nileshsah/letor-search-engine.svg?token=pVgo4dndv212ztXuejyy&branch=master)](https://travis-ci.com/nileshsah/letor-search-engine)

A stand-alone project for building a search engine completely from scratch over the wikipedia data corpus using Hadoop framework and RankLib learning to rank library.

The project is configured to run various hadoop jobs over the wikipedia database dump to process parameters such as:
   - Page Rank
   - TF-IDF
   - Page Index
   - Page Length
   
Example Usage: `hadoop jar letor-search-engine.jar com.nellex.hadoop.WikiIndexProcessing`

Currently the project utilizes a SQL database for storing the index table, the schema for which is presented here [artifacts/Wiki.sql](artifacts/Wiki.sql).

Various helper classes has been provided under the `com.nellex.helpers` package for parsing the generated output from the hadoop runs into the required schema.

We make use of the [RankLib](https://sourceforge.net/p/lemur/wiki/RankLib/) library as our ranking engine by first training it on a collection of labeled dataset as generated via the python script [search.py](artifacts/python-dataset-generator/search.py) which in turn works by querying the wikipedia search API with a randomly sampled set of unigrams and bi-grams.

The various features used for training the model includes, (1) query id (2) covered query terms in the title (3) title length (4) covered query term ratio for the title (5) length of the document (6) PageRank score (7) occurrence count of the query terms in the document (8) sum of term frequency for the query term in the document (8) minimum of the term frequency for the query term in the document, similarly (9) maximum of term frequency (10) mean of term frequency (11) sum of tf * idf (12) min of tf * idf (13) max of tf * idf (14) mean of tf  *idf (15) IDF.

## License
MIT License.  See [the license](LICENSE) for more details.
