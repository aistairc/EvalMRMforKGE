# EvalMRMforKGE
TBW  

## Installation

1. Install [PyTorch](https://pytorch.org/get-started/locally/)

2. Clone
```bash
git clone https://github.com/aistairc/EvalMRMforKGE.git
cd EvalMRMforKGE
cd openke
```
3. Compile C++ files
```bash
bash make.sh
```	
4. Quick Start  

[KGRC-RDF](https://github.com/KnowledgeGraphJapan/KGRC-RDF/tree/master/2020v2)
```bash
python train_transe_KGRC.py
python train_transh_KGRC.py
```
RDF-star representation of KGRC-RDF
```bash
python train_transe_KGRC_RDF-star.py
python train_transh_KGRC_RDF-star.py
```
RDF-star+ID representation of KGRC-RDF
```bash
python train_transe_KGRC_RDF-star-ext.py
python train_transh_KGRC_RDF-star-ext.py
```
## Preprocess 

Convert KGRC-RDF (Event-centric model) to RDF-star
```bash
cd MRMConverter
java -jar MRMConverter.jar kgrc_all.nt 0
```
rdf-star.ttl is generated.

Convert KGRC-RDF (Event-centric model) to RDF-star+ID  
RDF-star+ID distinguishes same quoted triples that occur in different contexts.
```bash
java -jar MRMConverter.jar kgrc_all.nt 1
```
rdf-star\_ext.ttl is generated.

## Data

## To do

