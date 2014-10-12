/*
 TagRecommender:
 A framework to implement and evaluate algorithms for the recommendation
 of tags.
 Copyright (C) 2013 Dominik Kowald
 
 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as
 published by the Free Software Foundation, either version 3 of the
 License, or (at your option) any later version.
 
 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.
 
 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package processing;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import com.google.common.primitives.Ints;

import java.text.DecimalFormat;

import file.PredictionFileWriter;
import file.BookmarkReader;
import common.Bookmark;
import common.DoubleMapComparator;

import com.google.common.base.Stopwatch;
import com.google.common.primitives.Ints;

import common.DoubleMapComparator;
import common.Bookmark;
import common.Utilities;
import file.PredictionFileWriter;
import file.BookmarkReader;


import no.uib.cipr.matrix.VectorEntry;
import no.uib.cipr.matrix.sparse.SparseVector;

import org.grouplens.lenskit.vectors.*;


public class ContentBasedCalculator {

	private final static int REC_LIMIT = 10;
	
	private BookmarkReader reader;
	private List<Bookmark> trainList;
	
	private List<Map<Integer, Double>> userMaps;
	private List<Map<Integer, Double>> tagRecencies;
	private List<Map<Integer, Double>> resMaps;
	
	public ContentBasedCalculator(BookmarkReader reader, int trainSize) {
		this.reader = reader;
		
		// TODO: use this data for recommendations
		this.trainList = this.reader.getBookmarks().subList(0, trainSize);
	}	
	
	public Map<Integer, Double> getRankedTagList(int userID, int resID) {
		
		Map<Integer, Double> resultMap = new LinkedHashMap<Integer, Double>();
		this.tagRecencies = new ArrayList<Map<Integer, Double>>();
		// TODO: calculate your recommendations here and return the top-10 (=REC_LIMIT) tags with probability value
		// have also a look on the other calculator classes!
		
			if (this.userMaps != null && userID < this.userMaps.size()) {
				Map<Integer, Double> userMap = this.userMaps.get(userID);
				Map<Integer, Double> recMap = this.tagRecencies.get(userID);
				for (Map.Entry<Integer, Double> entry : userMap.entrySet()) {
					resultMap.put(entry.getKey(), entry.getValue().doubleValue() * recMap.get(entry.getKey()).doubleValue());
				}
			}
			if (this.resMaps != null && resID < this.resMaps.size()) {
				Map<Integer, Double> resMap = this.resMaps.get(resID);
				for (Map.Entry<Integer, Double> entry : resMap.entrySet()) {
					Double val = resultMap.get(entry.getKey());
					resultMap.put(entry.getKey(), val == null ? entry.getValue().doubleValue() : val.doubleValue() + entry.getValue().doubleValue());
				}
			}
		
			Map<Integer, Double> returnMap = new LinkedHashMap<Integer, Double>();
			Map<Integer, Double> sortedResultMap = new TreeMap<Integer, Double>(new DoubleMapComparator(resultMap));
			sortedResultMap.putAll(resultMap);
			int count = 0;
			for (Map.Entry<Integer, Double> entry : sortedResultMap.entrySet()) {
				if (count++ < 10) {
					returnMap.put(entry.getKey(), entry.getValue());
				} else {
					break;
				}
			}		
			return returnMap;

		//Ahora calculo en base a TF-IDF 
			
			//DecimalFormat df = new DecimalFormat("0.0000");
			
			// Obtener el perfil del usuario con el "Me Gusta" para cada etiqueta
			
			//SparseVector userVector = makeUserVector(userID);
	
			// Loop sobre cada elemento requerido y su puntuación
			// El dominio del vector de salida es lo que se conseguirá
			
			//for (VectorEntry e: resID.fast(VectorEntry.State.EITHER))
			//{
				// El Score del item representado por e.
				// Obtener el vector del item para este item en particular
				
			//	SparseVector iv = model.getItemVector(e.getKey());
				// Se calcula el coseno del item y el perfil del ususario
				//Se guarda en el vector de salida
	
			//	double numerator = iv.dot(userVector);
			//	double denominator = iv.norm() * userVector.norm();
			//	double cosineSimilarity = numerator / denominator;
			//	try
			//	{
			//		double rounded = df.parse(df.format(cosineSimilarity)).doubleValue();
			//		logger.info("Cos similarity for item {} and user {} is {}", e.getKey(), userID, rounded);
	
			//		resID.set(e.getKey(), rounded);
			//	}
			//	catch (ParseException e1)
			//	{
			//		logger.error("Error while parsing double", e1);
			//	}
			//}
	}
	// ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
	
	public static List<Map<Integer, Double>> startContentBasedCreation(BookmarkReader reader, int sampleSize) {
		int size = reader.getBookmarks().size();
		int trainSize = size - sampleSize;
		
		ContentBasedCalculator calculator = new ContentBasedCalculator(reader, trainSize);
		List<Map<Integer, Double>> results = new ArrayList<Map<Integer, Double>>();
		if (trainSize == size) {
			trainSize = 0;
		}
		
		for (int i = trainSize; i < size; i++) { // the test-set
			Bookmark data = reader.getBookmarks().get(i);
			Map<Integer, Double> map = calculator.getRankedTagList(data.getUserID(), data.getWikiID());
			results.add(map);
		}
		return results;
	}
	
	public static void predictSample(String filename, int trainSize, int sampleSize) {

		BookmarkReader reader = new BookmarkReader(trainSize, false);
		reader.readFile(filename);

		List<Map<Integer, Double>> modelValues = startContentBasedCreation(reader, sampleSize);
		
		List<int[]> predictionValues = new ArrayList<int[]>();
		for (int i = 0; i < modelValues.size(); i++) {
			Map<Integer, Double> modelVal = modelValues.get(i);
			predictionValues.add(Ints.toArray(modelVal.keySet()));
		}
		String suffix = "_cb";
		reader.setUserLines(reader.getBookmarks().subList(trainSize, reader.getBookmarks().size()));
		PredictionFileWriter writer = new PredictionFileWriter(reader, predictionValues);
		String outputFile = filename + suffix;
		writer.writeFile(outputFile);
	}
}
