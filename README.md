# EvalMRMforKGE
Evaluation codes for metadata representation models in event-centric knowledge graph embedding  

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

## Data
[KGRC-RDF](https://github.com/KnowledgeGraphJapan/KGRC-RDF/tree/master/2020v2)  
Please see [Report on the First Knowledge Graph Reasoning Challenge 2018](https://link.springer.com/chapter/10.1007/978-3-030-41407-8_2) and [International Knowledge Graph Reasoning Challenge](https://ikgrc.org/).

## Preprocess 

1. Prepare ttl files
Convert KGRC-RDF (Event-centric model) to RDF-star
```bash
cd MRMConverter
java -jar MRMConverter.jar kgrc_all.nt rdr 0
```
rdf-star.ttl is generated.

Convert KGRC-RDF (Event-centric model) to RDF-star+ID  
RDF-star+ID distinguishes same quoted triples that occur in different contexts.
```bash
java -jar MRMConverter.jar kgrc_all.nt rdr 1
```
rdf-star\_ext.ttl is generated.  

Convert KGRC-RDF (Event-centric model) to Singleton Property  
```bash
java -jar MRMConverter.jar kgrc_all.nt sgprop
```
2. Load ttl files into a triplestore.  

3. Get tsv files using the below SPARQL queries.

For Event-centric model:
```sql
PREFIX kgc: <http://kgc.knowledge-graph.jp/ontology/kgc.owl#>
SELECT DISTINCT ?s ?p ?o
WHERE {
  ?s ?p ?o .
  filter(isURI(?o))
  BIND(RAND() AS ?sortKey)
} ORDER BY ?sortKey
```

For RDF-star and RDF-star+ID:
```sql
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
SELECT DISTINCT ?s ?p ?o
WHERE {{
        ?s ?p ?o .
    } union {
        <<?s ?p ?o>> ?p2 ?o2 .
        filter(!isLiteral(?o2))
    } union {
        << << ?s ?p ?o>> rdf:value ?v >> ?p2 ?o2 .
        filter(!isLiteral(?o2)) }
    filter(!isLiteral(?o))
    BIND(RAND() AS ?sortKey)
} ORDER BY ?sortKey
```

4. Create {entity, relation, train, valid, test}2id.txt
Create directories
```bash
mkdir benchmarks/KGRC
mkdir benchmarks/KGRC-RDF-star
mkdir benchmarks/KGRC-RDF-star-ext
mkdir export
```
Create id files using [ToolsForFastTransX](https://github.com/KnowledgeGraphJapan/KGRC-Tools/tree/master/ToolsforFastTransX)

```bash
java -jar URI2ID.jar kgrc_all.tsv 8 1 1
```
Locate id files
```bash
cp export/*.txt benchmarks/KGRC/
```
Create type\_constraint.txt
```bash
cp benchmarks/FB15K/n-n.py benchmarks/KGRC/
cd benchmarks/KGRC
python n-n.py
```
## Publications
under submission
