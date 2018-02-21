package com.msc.minifreemarker;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author micky
 */
public class MiniFreeMarker {

    private Map<String, Object> root;
    private final List<String> errors = new ArrayList<>();
    private final String reader;

    public MiniFreeMarker(InputStream is) throws IOException {
	InputStreamReader isr = new InputStreamReader(is);
	BufferedReader br = new BufferedReader(isr);
	StringWriter sw = new StringWriter();
	String line;
	while( (line=br.readLine()) != null){
	    sw.write(line);
	    sw.write("\n");
	}
	is.close();	
        reader = sw.toString();
	sw = null;
	br = null;
	isr = null;
    }

    public List<String> getError() {
        return errors;
    }

    public void putData(Map<String, Object> root) {
        this.root = root;
    }

    private List<Pregs> getDollarsValues(String line) {
        List<Pregs> listes = new ArrayList<>();
        StringBuilder sb1;
        char[] lines = line.toCharArray();
        for (int i = 0; i < lines.length; i++) {
            if (lines[i] == '$') {
                i += 2; // = {a
                sb1 = new StringBuilder();
                Pregs preg = new Pregs();
                while (lines[i] != '}') {
                    sb1.append(lines[i]);
                    i++;
                }
                preg.originalKey = sb1.toString();
                preg.split();
                listes.add(preg);
            }
        }
        return listes;
    }

    private String getGoodValue(String field, Object o) {
        try {
            Method[] ms = o.getClass().getDeclaredMethods();
            for (Method m : ms) {
                if (m.getName().toLowerCase().contains(field.toLowerCase())) {
                    return m.invoke(o).toString();
                }
            }
            Field fields[] = o.getClass().getDeclaredFields();
            for (Field f : fields) {
                if (f.getName().equalsIgnoreCase(field.toLowerCase())) {
                    f.setAccessible(true);
                    return f.get(o).toString();
                }
            }
        } catch (Exception e) {
            errors.add(e.getMessage());
        }
        return null;
    }

    private String replace(String line, Map<String, Object> map) {
        List<Pregs> ls = getDollarsValues(line);
        String receptacle;
        for (Pregs preg : ls) {
            Object val = map.get(preg.key);
            if (preg.values != null) {
                receptacle = getGoodValue(preg.values, val);
            } else {
                receptacle = val.toString();
            }
            line = line.replace("${" + preg.originalKey + "}", receptacle);
        }
        return line;
    }

    //${list lists as other?counter}
    //return [0]lists and [1]other [2] true/false counter
    private String[] parseList(String listLine) {
        int pos1 = "${list ".length();
        int pos2 = listLine.indexOf(" as ");
        int pos3 = listLine.indexOf("?");
        boolean counter = false;
        if (pos3 == -1) {
            pos3 = listLine.length() - 1;
        } else {
            counter = listLine.toLowerCase().contains("counter");
        }

        String[] strs = new String[3];
        strs[0] = listLine.substring(pos1, pos2);
        strs[1] = listLine.substring(pos2 + 4, pos3);
        strs[2] = "" + counter;
        return strs;
    }

    public String process() throws IOException {
        StringReader reader2 = new StringReader(this.reader);
        BufferedReader br = new BufferedReader(reader2);
        StringWriter sw = new StringWriter();
        String line;
        while ((line = br.readLine()) != null) {
            if (line.contains("${")) {
                if (line.contains("${list")) {
                    List<String> sb = new ArrayList<>();
                    //on extrait la ligne ${list xxx as yyy}
                    String[] list = parseList(line.trim());
                    //on copie les ligne entre ${list xxx as yyyy} et /list
                    //pour les travbailler 1 par 1 juste apres
                    while ((line = br.readLine()) != null && !line.contains("${/list}")) {
                        sb.add(line);
                    }
                    Map<String, Object> map;
                    //on recupere la VRAI liste grace a la map root
                    List nblines = (List) root.get(list[0]);
                    boolean counter = false;
                    int count = 1;
                    if (list[2].equals("true")) {
                        counter = true;
                    }
//puis le nonmbre de list on boucle sur les lignes mise en cache plus tot
                    //pour les travailler 1 par une.
                    for (Object nb : nblines) {
                        map = new HashMap<>();
                        map.put(list[1], nb);
                        if (counter) {
                            map.put("counter", count);
                            count++;
                        }
                        for (String theLine : sb) {
                            if (theLine.contains("${")) {
                                sw.append(replace(theLine, map));
                            } else {
                                sw.append(theLine);
                            }
                            sw.append('\n');
                        }
                    }
                } else {
                    line = replace(line, root);
                }
            }
            if (line.contains("${/")) {
                continue;
            }
            sw.append(line);
            sw.append('\n');
        }	
        return sw.toString();
    }

}
