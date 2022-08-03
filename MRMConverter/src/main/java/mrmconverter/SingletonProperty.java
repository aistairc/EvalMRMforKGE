package mrmconverter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;

public class SingletonProperty {
	final static String kgc = "http://kgc.knowledge-graph.jp/ontology/kgc.owl#";
	final static String ex = "http://example.com/";
	
	public static ArrayList<Resource> expandNestStatement(Map<String,ArrayList<Metadata>> stmt_map, String obj, Model model, Map<String,Integer> sgp_cnt_map, Map<String,Property> scene_sgp_map) {
		ArrayList<Resource> obj_r_list = new ArrayList<Resource>();
		Resource obj_r = null;
		
		String[] uri_split = obj.split("/");
		String id = uri_split[uri_split.length-1];
		Pattern pattern = Pattern.compile("^[0-9]{3}$|^[0-9]{2}$");
		Matcher matcher = pattern.matcher(id);
		
		// if obj is scene resource
		if (matcher.find()) {
//			System.out.println(obj);
			ArrayList<Metadata> _stmt_list = stmt_map.get(obj);
			try {
				// Scenes can have multiple subjects and multiple objects
				for (Metadata _stmt : _stmt_list) {
					String _subj = _stmt.getSubj();
					String _pred = _stmt.getPred();
					String _obj = _stmt.getObj();
					String scene = _stmt.getScene();
					Resource _subj_r = model.getResource(_subj);
					Property pred_sgp = getSingletonProperty(model, _pred, scene, scene_sgp_map, sgp_cnt_map);
					
					ArrayList<Resource> _obj_r_list = expandNestStatement(stmt_map, _obj, model, sgp_cnt_map, scene_sgp_map);
					for (Resource _obj_r : _obj_r_list) {
						_subj_r.addProperty(pred_sgp, _obj_r);
						pred_sgp.addProperty(RDF.value, model.createLiteral(scene));
						obj_r_list.add(pred_sgp);
						model.removeAll(model.getResource(scene), null, null);
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
	
	public static Property getSingletonProperty(Model model, String pred, String scene, Map<String,Property> scene_sgp_map, Map<String,Integer> sgp_cnt_map) {
		Property sgp = model.getProperty(ex + "singletonPropertyOf");
		Property pred_p = model.getProperty(pred);
		Property pred_sgp = null;
		int sgp_id = 1;
		if (model.contains(null, RDF.value, scene)) {
			//The statement resource has been already created.
			pred_sgp = scene_sgp_map.get(scene);
		} else {
			// check the number of singleton property
			if (sgp_cnt_map.containsKey(pred)) {
				sgp_id = sgp_cnt_map.get(pred);
				sgp_id += 1;
			} else {
				sgp_id = 1;
			}
			sgp_cnt_map.put(pred, sgp_id);
			// create a singleton property
			pred_sgp = model.createProperty(pred + "-" + Integer.toString(sgp_id));
			pred_sgp.addProperty(sgp, pred_p);
			scene_sgp_map.put(scene, pred_sgp);
		}
		return pred_sgp;
	}
	
	public static Model create(Model model, Map<String, ArrayList<Metadata>> stmt_map) {
		int sgp_id = 1;
		Map<String, Integer> sgp_cnt_map = new HashMap<String, Integer>();
		Map<String, Property> scene_sgp_map = new HashMap<String, Property>();
		Property sgp = model.createProperty(ex + "singletonPropertyOf");
		for (Entry<String, ArrayList<Metadata>> e : stmt_map.entrySet()) {
			String scene = e.getKey();
			ArrayList<Metadata> stmt_list = e.getValue();
			
			for (Metadata stmt : stmt_list) {
				String subj = stmt.getSubj();
				String pred = stmt.getPred();
				String obj = stmt.getObj();
				Map<String,String> metadata = stmt.getMetadata();
				
				Resource subj_r = model.getResource(subj);
				Property pred_sgp = getSingletonProperty(model, pred, scene, scene_sgp_map, sgp_cnt_map);
				
				// objがsceneの場合はsgpropを返す
				ArrayList<Resource> obj_r_list = expandNestStatement(stmt_map, obj, model, sgp_cnt_map, scene_sgp_map);
				
				for (Resource obj_r : obj_r_list) {
					subj_r.addProperty(pred_sgp, obj_r);
					pred_sgp.addProperty(RDF.value, model.createLiteral(scene));
					model.removeAll(model.getResource(scene), null, null);
					
					for (Entry<String, String> e2 : metadata.entrySet()) {
						String key = e2.getKey();
						String val = e2.getValue();
						Property key_p = null;
						if (key.equals("type")) {
							key_p = model.getProperty(RDF.uri + key);
						} else {
							key_p = model.getProperty(kgc + key);
						}
						
						ArrayList<Resource> val_r_list = expandNestStatement(stmt_map, val, model, sgp_cnt_map, scene_sgp_map);
						for (Resource val_r : val_r_list) {
							pred_sgp.addProperty(key_p, val_r);
						}
					}
					
				}
				
			}
			model.removeAll(model.getResource(scene), null, null);
		}
		model.setNsPrefix("ex", ex);
		return model;
	}

}
