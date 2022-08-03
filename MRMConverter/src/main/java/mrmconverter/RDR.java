package mrmconverter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;

public class RDR {
	final static String kgc = "http://kgc.knowledge-graph.jp/ontology/kgc.owl#";
	static String[] bugs = {
//			"http://kgc.knowledge-graph.jp/data/ACaseOfIdentity/489",
//			"http://kgc.knowledge-graph.jp/data/ACaseOfIdentity/546",
//			"http://kgc.knowledge-graph.jp/data/ACaseOfIdentity/312",
//			"http://kgc.knowledge-graph.jp/data/ACaseOfIdentity/161",
//			"http://kgc.knowledge-graph.jp/data/ACaseOfIdentity/526",
//			"http://kgc.knowledge-graph.jp/data/SilverBlaze/381",
//			"http://kgc.knowledge-graph.jp/data/SilverBlaze/380",
//			"http://kgc.knowledge-graph.jp/data/ACaseOfIdentity/437",
//			"http://kgc.knowledge-graph.jp/data/ACaseOfIdentity/502"
			};
	public static RDFNode getRDFStarStatement(String scene, Model model) {
		String sparql = "PREFIX kgc: <http://kgc.knowledge-graph.jp/ontology/kgc.owl#>\n"
				+ "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
				+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
				+ "SELECT DISTINCT * WHERE {\n"
				+ "?s rdf:value \"" + scene + "\" .\n"
				+ "}";
		Query query = QueryFactory.create(sparql);
		QueryExecution qe = QueryExecutionFactory.create(query, model);
		ResultSet results = qe.execSelect();
		RDFNode s = null;
		while (results.hasNext()) {
			QuerySolution result = results.next();
			s = result.get("s");
		}
		return s;
	}
	
	public static ArrayList<Resource> expandNestStatement(Map<String,ArrayList<Metadata>> stmt_map, String obj, Model model, String mode) {
		ArrayList<Resource> obj_r_list = new ArrayList<Resource>();
		Resource obj_r = null;
		
		String[] uri_split = obj.split("/");
		String id = uri_split[uri_split.length-1];
		Pattern pattern = Pattern.compile("^[0-9]{3}$|^[0-9]{2}$");
		Matcher matcher = pattern.matcher(id);
		
		// if obj is scene resource
		if (matcher.find() && !Arrays.asList(bugs).contains(obj)) {
//			System.out.println(obj);
			ArrayList<Metadata> _stmt_list = stmt_map.get(obj);
			try {
				for (Metadata _stmt : _stmt_list) {
					String _subj = _stmt.getSubj();
					String _pred = _stmt.getPred();
					String _obj = _stmt.getObj();
					String scene = _stmt.getScene();
					Resource _subj_r = model.getResource(_subj);
					Property _pred_r = model.getProperty(_pred);
					ArrayList<Resource> _obj_r_list = expandNestStatement(stmt_map, _obj, model, mode);
					for (Resource _obj_r : _obj_r_list) {
						obj_r = createRDFStarStatement(_subj_r, _pred_r, _obj_r, scene, model, mode); 
						obj_r_list.add(obj_r);
					}
				}
			} catch (Exception e) {
				// issues on the kg side
//				e.printStackTrace();
				obj_r = model.getResource(obj);
				obj_r_list.add(obj_r);
			}
		} else {
			obj_r = model.getResource(obj);
			obj_r_list.add(obj_r);
		}
		return obj_r_list;
	}
	
	public static Resource createRDFStarStatement(Resource subj_r, Property pred_r, Resource obj_r, String scene, Model model, String mode) {
		Statement _rdf_stmt = model.createStatement(subj_r, pred_r, obj_r);
		Resource _rdf_stmt_r = model.createResource(_rdf_stmt);
		Statement rdf_stmt = null;
		Resource rdf_stmt_r = null;

		// Not distinguish quoted triples
		if (mode.equals("0")) {
			rdf_stmt_r = _rdf_stmt_r;
			rdf_stmt_r.addProperty(RDF.value, model.createLiteral(scene));
		} 
		// Distinguish quoted triples by id
		else {
			rdf_stmt = model.createStatement(_rdf_stmt_r, RDF.value, scene);
			rdf_stmt_r = model.createResource(rdf_stmt);
		}
		
		model.removeAll(model.getResource(scene), null, null);
		
		return rdf_stmt_r;
	}
	
	public static Model create(Model model, Map<String, ArrayList<Metadata>> stmt_map, String mode) {
		for (Entry<String, ArrayList<Metadata>> e : stmt_map.entrySet()) {
			
			String scene = e.getKey();
			ArrayList<Metadata> stmt_list = e.getValue();
			
			for (Metadata stmt : stmt_list) {
				String subj = stmt.getSubj();
				String pred = stmt.getPred();
				String obj = stmt.getObj();
				Map<String,String> metadata = stmt.getMetadata();
				
				if (model.contains(null, RDF.value, scene)) {
					//The statement resource has been already created.
//					continue;
				}
				
				Resource subj_r = model.getResource(subj);
				Property pred_r = model.getProperty(pred);
				ArrayList<Resource> obj_r_list = expandNestStatement(stmt_map, obj, model, mode);
				
				// There can be more than one object.
				for (Resource obj_r : obj_r_list) {
					Resource rdf_stmt_r = createRDFStarStatement(subj_r, pred_r, obj_r, scene, model, mode);

					// Metadata of RDFStar statement resource
					for (Entry<String,String> e2 : metadata.entrySet()) {
						String key = e2.getKey();
						Property key_p = null;
						if (key.equals("type")) {
							key_p = model.getProperty(RDF.uri + key);
						} else {
							key_p = model.getProperty(kgc + key);
						}
						String val = e2.getValue();
						ArrayList<Resource> val_r_list = expandNestStatement(stmt_map, val, model, mode);
						for (Resource val_r : val_r_list) {
							rdf_stmt_r.addProperty(key_p, val_r);
						}
					}
				}
			}
			
		}
		return model;
	}
}
