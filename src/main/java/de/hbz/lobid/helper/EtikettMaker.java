/*Copyright (c) 2015 "hbz"

This file is part of lobid-rdf-to-json.

etikett is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as
published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.hbz.lobid.helper;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeBase;

/**
 * @author Jan Schnasse
 *
 */
public class EtikettMaker {

	final static Logger logger = LoggerFactory.getLogger(EtikettMaker.class);

	/**
	 * A map with URIs as key
	 */
	Map<String, Etikett> pMap = new HashMap<>();

	/**
	 * A map with Shortnames as key
	 */
	Map<String, Etikett> nMap = new HashMap<>();

	/**
	 * The context will be loaded on startup. You can reload the context with POST
	 * /utils/reloadContext
	 * 
	 */
	Map<String, Object> context = new HashMap<>();

	/**
	 * The labels will be loaded on startup. You can reload the context with POST
	 * /utils/reloadLabels
	 * 
	 */
	List<Etikett> labels = new ArrayList<>();

	/**
	 * The profile provides a json context an labels
	 * 
	 * @param labelIn input stream to a labels file
	 */
	public EtikettMaker(InputStream labelIn) {
		initMaps(labelIn);
		initContext();
	}

	/**
	 * @return a map with a json-ld context
	 */
	public Map<String, Object> getContext() {
		return context;
	}

	/**
	 * @param key the uri
	 * @return an etikett object contains uri, icon, label, jsonname,
	 *         referenceType
	 */
	public Etikett getEtikett(String uri) {
		Etikett e = pMap.get(uri);
		if (e == null) {
			e = new Etikett(uri);
			e.name = getJsonName(uri);
		}
		if (e.label == null) {
			e.label = e.uri;
		}
		logger.debug("Find etikett for " + uri + " : " + e.name);
		return e;
	}

	private void initContext() {
		context = createContext();
	}

	private void initMaps(InputStream labelIn) {
		try {
			labels = createLabels(labelIn);

			for (Etikett etikett : labels) {
				pMap.put(etikett.uri, etikett);
				nMap.put(etikett.name, etikett);
			}
		} catch (Exception e) {
			logger.debug("", e);
		}

	}

	private static List<Etikett> createLabels(InputStream labelIn) {
		logger.info("Create labels....");
		List<Etikett> result = new ArrayList<>();

		result = loadFile(labelIn, new ObjectMapper().getTypeFactory()
				.constructCollectionType(List.class, Etikett.class));

		if (result == null) {
			logger.info("...not succeeded!");
		} else {
			logger.info("...succeed!");
		}
		return result;
	}

	Map<String, Object> createContext() {
		Map<String, Object> pmap;
		Map<String, Object> cmap = new HashMap<String, Object>();
		for (Etikett l : labels) {
			if ("class".equals(l.referenceType) || l.referenceType == null
					|| l.name == null)
				continue;
			pmap = new HashMap<String, Object>();
			pmap.put("@id", l.uri);
			if (!"String".equals(l.referenceType)) {
				pmap.put("@type", l.referenceType);
			}
			if (l.container != null) {
				pmap.put("@container", l.container);
			}
			cmap.put(l.name, pmap);
		}
		cmap.put("id", "@id");
		cmap.put("type", "@type");
		Map<String, Object> contextObject = new HashMap<String, Object>();
		contextObject.put("@context", cmap);
		return contextObject;
	}

	private static <T> T loadFile(InputStream labelIn, TypeBase type) {
		try (InputStream in = labelIn) {
			return new ObjectMapper().readValue(in, type);
		} catch (Exception e) {
			throw new RuntimeException("Error during initialization!", e);
		}
	}

	/**
	 * @param predicate
	 * @return The short name of the predicate uses String.split on first index of
	 *         '#' or last index of '/'
	 */
	String getJsonName(String predicate) {
		Etikett e = pMap.get(predicate);
		if (e == null) {
			throw new RuntimeException(predicate
					+ ": no json name available. Please provide a labels.json file with proper 'name' entry.");
		}
		return e.name;
	}

	/**
	 * @return a map with all etikett accessible over their json names
	 */
	public Map<String, Etikett> getNameMap() {
		return nMap;
	}
}
