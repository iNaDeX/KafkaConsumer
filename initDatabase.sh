#!/bin/bash

# create indices
curl -XPUT "http://localhost:9200/locations"
curl -XPUT "http://localhost:9200/sentiments"
curl -XPUT "http://localhost:9200/hashtags"
curl -XPUT "http://localhost:9200/keywords"

# create types
curl -XPUT "http://localhost:9200/locations/_mapping/locationsType" -d'
{
 "locationsType": {
  "properties": {
   "country": {"type": "keyword"},
   "geolocation": {"type": "geo_point"},
   "time": {"type": "date"}
  }
 }
}'

curl -XPUT "http://localhost:9200/sentiments/_mapping/sentimentsType" -d'
{
 "sentimentsType": {
  "properties": {
   "sentiment": {"type": "keyword"},
   "time": {"type": "date"}
  }
 }
}'

curl -XPUT "http://localhost:9200/hashtags/_mapping/hashtagsType" -d'
{
 "hashtagsType": {
  "properties": {
   "hashtags": {"type": "text", "fielddata": true},
   "time": {"type": "date"}
  }
 }
}'


curl -XPUT "http://localhost:9200/keywords/_mapping/keywordsType" -d'
{
 "keywordsType": {
  "properties": {
   "keywords": {"type": "text", "fielddata": true},
   "time": {"type": "date"}
  }
 }
}'
