# 'Learning to Rank' (LETOR) Text based Search Engine 
[![Build Status](https://travis-ci.com/nileshsah/letor-search-engine.svg?token=pVgo4dndv212ztXuejyy&branch=master)](https://travis-ci.com/nileshsah/letor-search-engine)

A stand-alone project for building a search engine completely from scratch over the wikipedia data corpus using Hadoop framework and RankLib learning to rank library.

The project is configured to run various hadoop jobs over the wikipedia database dump to process parameters such as:
   - Page Rank
   - TF-IDF
   - Page Index
   - Page Length
   
Example Usage: `hadoop jar letor-search-engine.jar com.nellex.hadoop.WikiIndexProcessing`
