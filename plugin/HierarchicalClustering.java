package plugin;

import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.Vector;

public class HierarchicalClustering {

    /**
     * Unknown usage
     * Note: values are missing from data if they have score 0
     * @param originalScores
     * @param rowSize
     * @param colSize
     * @param rows
     * @param symmetricalScores
     * @return 
     */
    public static Vector<Integer> Cluster (Vector<Vector<Double>> originalScores, 
            int rowSize, int colSize, boolean rows, boolean symmetricalScores)
    {
        HashMap<Integer, Vector<Integer>> clusterToGenes = new HashMap<Integer, Vector<Integer>>();
        Vector<Integer> toReturn = new Vector<Integer>();
        Vector<Vector<Double>> scores = new Vector<Vector<Double>>();
        // Make a score copy
        if (rows){
            for (int row = 0; row < rowSize; row++){
                Vector<Double> newVect = new Vector<Double>();
                for (int col = 0; col < colSize; col ++){
                        newVect.add(originalScores.get(row).get(col));
                }
                scores.add(newVect);
            }
        }
        else{
            // If doing columns (switch columns to rows)
            for (int col = 0; col < colSize; col++){
                Vector<Double> newVect = new Vector<Double>();
                for (int row = 0; row < rowSize; row ++){
                    newVect.add(originalScores.get(row).get(col));
                }
                scores.add(newVect);
            }
            int temp = rowSize;
            rowSize = colSize;
            colSize = temp;
        }

        Vector<Vector<Double>> distmatrix = getDistanceMatrix(scores, rowSize, colSize);

        // Maps the interacting pair to their score
        HashMap<String, Double> originalScoreMap = new HashMap<String, Double>();


        //================================SETUP======================================

        for (int row = 0; row < rowSize; row++){
            for (int col = 0; col < row; col++){
                // col is smaller! Note that the distance matrix is symmetrical!
                if (distmatrix.get(row).get(col) != null){
                    originalScoreMap.put(col+"//"+row, distmatrix.get(row).get(col));
                }
            }	
        }

        // Change all the scores that are 0 to null in the score matrix
        for (Vector<Double> row : scores){
            for (int col = 0; col < row.size(); col ++){
                Double d = row.get(col);
                if (d != null && d == 0){
                    row.set(col, null);
                }
            }
        }

      /* Setup a list specifying to which cluster a gene belongs, and keep track
       * of the number of elements in each cluster (needed to calculate the average). */
      for (int j = 0; j < rowSize; j++){ 
          Vector<Integer> newVect = new Vector<Integer>();
          newVect.add(j);
          clusterToGenes.put(j, newVect);
      }

      //=============================================================================

      //=========================REPITITIONS========================================

      // only 1 item
      if (rowSize == 1){
          toReturn.add(0);
      }
      // Repeatedly find the smallest interaction score, and combine the two parts involved into a pseudo-part
      for (int x = rowSize; x > 1; x--){ 
          int total_size, size_is, size_js;
          Integer [] vals = find_closest_pair(rowSize, distmatrix);

          Integer is = vals[0];
          Integer js = vals[1];

          // A smallest distance pair was not found (most likely because only missing values are left in the table)
          // In this case assign i and j random complex numbers so that they are combined
          if (is == null && js == null){
            for (Integer clustNum  : clusterToGenes.keySet()){
                if (is == null)
                    is = clustNum;
                else if (js == null){
                    js = clustNum;
                }
                else{
                    break;
                }
            }
            // Update clusterToGenes with the new cluster
            Vector<Integer> iClusterParts = clusterToGenes.get(is);
            Vector<Integer> jClusterParts = clusterToGenes.get(js);

            clusterToGenes.put(is, getOrderedCluster(iClusterParts, jClusterParts, originalScoreMap));
            clusterToGenes.remove(js);

            toReturn = clusterToGenes.get(is);
            continue;
            }

            // Combine scores into i scores (making i the new combined complex)
            // Change all interactions with js to null (no longer relevant)
            size_is = clusterToGenes.get(is).size();
            size_js = clusterToGenes.get(js).size();
            total_size = size_is + size_js;

            // Combining rows

            for (int count = 0; count < colSize; count ++){
                Double val_is_count = scores.get(is).get(count);
                Double val_js_count = scores.get(js).get(count);

                if (val_is_count != null && val_js_count != null){
                  scores.get(is).set(count, (val_is_count*size_is + val_js_count*size_js)/total_size);
                }
                else if (val_is_count == null && val_js_count != null){
                  scores.get(is).set(count, val_js_count);
                }
                // Otherwise either is_j != null and val_js_j == null -> leave as is
                scores.get(js).set(count, null);
            }

            // Combining columns (if both the same)
            if (symmetricalScores){
                for (int count = 0; count < rowSize; count ++){
                    if (count == is){
                           continue;
                    }
                    Double val_count_is = scores.get(count).get(is);
                    Double val_count_js = scores.get(count).get(js);
                    if (val_count_is != null && val_count_js != null){
                        scores.get(count).set(is, (val_count_is*size_is + val_count_js*size_js)/total_size);
                    }
                    else if (val_count_is == null && val_count_js != null){
                        scores.get(count).set(is, val_count_js);
                    }
                    // Otherwise leave it
                    scores.get(count).set(js, null);
                }
            }

            // Remove the jth row/column from distancematrix (the complex is no longer active
            for (int count = 0; count < js; count ++){
                distmatrix.get(js).set(count, null);
            }
            for (int count = js+1; count < scores.size(); count ++){
                distmatrix.get(count).set(js, null);
            }

            // Update clusterToGenes with the new cluster
            Vector<Integer> iClusterParts = clusterToGenes.get(is);
            Vector<Integer> jClusterParts = clusterToGenes.get(js);

            clusterToGenes.put(is, getOrderedCluster(iClusterParts, jClusterParts, originalScoreMap));
            clusterToGenes.remove(js);

            // Recalculate the interaction scores for the new pseudogene, and place them in the distance matrix
            recalculateDistances (is, clusterToGenes.keySet(), scores, colSize, distmatrix);

            toReturn = clusterToGenes.get(is);
        }//END FOR
        return toReturn;
    }

    /**
     * When combining two clusters of genes, this method returns them in 
     * the optimal order (so that they are joined where the score is the highest!
     * @param iClusterParts
     * @param jClusterParts
     * @param originalScoreMap
     * @return 
     */
    private static Vector<Integer> getOrderedCluster(Vector<Integer> iClusterParts,
        Vector<Integer> jClusterParts, HashMap<String, Double> originalScoreMap){
        int first_i_complex = iClusterParts.get(0);
        int last_i_complex = iClusterParts.get(iClusterParts.size()-1);

        int first_j_complex = jClusterParts.get(0);
        int last_j_complex = jClusterParts.get(jClusterParts.size()-1);

        Double first_score = originalScoreMap.get(Math.min(first_i_complex,
                first_j_complex)+ "//" + Math.max(first_i_complex, first_j_complex));
        Double last_score = originalScoreMap.get(Math.min(last_i_complex,
                last_j_complex)+ "//" + Math.max(last_i_complex, last_j_complex));
        Double inner_score = originalScoreMap.get(Math.min(last_i_complex,
                first_j_complex)+ "//" + Math.max(last_i_complex, first_j_complex));
        Double outer_score = originalScoreMap.get(Math.min(first_i_complex,
                last_j_complex)+ "//" + Math.max(first_i_complex, last_j_complex));


        Vector<Integer> newComplexList = new Vector<Integer>();
        // Rearrange the part order as necessary

        // first_score is smallest
        if (first_score != null &&
                    (last_score == null || first_score <= last_score) && 
                    (inner_score == null || first_score <= inner_score) && 
                    (outer_score == null || first_score <= outer_score)){
            Collections.reverse(iClusterParts);
        }
        else if (last_score != null &&
                    (inner_score == null || last_score <= inner_score) && 
                    (outer_score == null || last_score <= outer_score)){
            Collections.reverse(jClusterParts);
        }
        else if (outer_score != null &&
                    (inner_score == null || outer_score <= inner_score)){
            Collections.reverse(iClusterParts);
            Collections.reverse(jClusterParts);
        }
        // if inner score is the smallest, just combining the two lists gives the optimal order!
        newComplexList.addAll(iClusterParts);
        newComplexList.addAll(jClusterParts);

        return newComplexList;
    }

    /**
     * Recalculate distance matrix?
     * @param cluster_num
     * @param activeClusters
     * @param scores
     * @param colSize
     * @param distmatrix 
     */
    private static void recalculateDistances (int cluster_num, 
        Set<Integer> activeClusters, Vector<Vector<Double>> scores,
        int colSize, Vector<Vector<Double>> distmatrix){
        Vector<Double> vectorx = new Vector<Double>();
        // Populate vectorx using cluster_num
        for (int count = 0; count < colSize; count ++){
            vectorx.add(scores.get(cluster_num).get(count));
        }
        for (Integer complex : activeClusters){
            Vector<Double> vectory = new Vector<Double>();
            if (complex.intValue() != cluster_num){
                // Populate vectory using complex
                for (int count = 0; count < colSize; count ++){
                    vectory.add(scores.get(complex).get(count));
                }
                distmatrix.get(Math.max(complex, cluster_num)).set(Math.min(complex, cluster_num), pearsonDistance(vectorx, vectory));
            }
        }
    }


    /**
     * Calculates the distance matrix between genes using their measured 
     * gene expression data. As the distance matrix is symmetric, with zeros on
     * the diagonal, only the lower triangular half of the distance matrix is saved.
     * @param scores
     * @param rowSize
     * @param colSize
     * @return 
     */
    private static Vector<Vector<Double>> getDistanceMatrix (Vector<Vector<Double>> scores, int rowSize, int colSize)
    {
      Vector<Vector<Double>> matrix = new Vector<Vector<Double>>();
      /* Calculate the distances and save them in the array */
      for (int i = 0; i < rowSize; i++){
          Vector<Double> row = new Vector<Double>();
          for (int j = 0; j < i; j++){
              Double val = average_pearson(scores,i,j);
              row.add(val);
          }
          row.add(0.0);
          matrix.add(row);
      }
      return matrix;
    }

    /**
     * Average Pearson correlation..
     * @param scores
     * @param x
     * @param y
     * @return 
     */
    private static Double average_pearson (Vector<Vector<Double>>scores, int x, int y){
        if (scores.elementAt(x) != null && scores.elementAt(y) != null){
            Vector<Double> vectorx = scores.get(x);
            Vector<Double> vectory = scores.get(y);
            return pearsonDistance(vectorx, vectory);
        }
        else{
            return null;
        }
    }

    /**
     * Calculates the Pearson distance value between the two vectors
     * @param vectorx
     * @param vectory
     * @return Pearson correlation between two vectors, null otherwise
     */
    public static Double pearsonDistance (Vector<Double> vectorx, Vector<Double> vectory){
        double s_xy = 0, s_x = 0, s_y = 0;
        double sum = 0;
        double total = 0;
        for (Double d : vectorx){
            if (d!= null && d != 0){
                sum += (d*d);
                total ++;
            }
        }
        s_x = Math.sqrt(sum/total);
        if (sum == 0 || total == 0){
            return null;
        }
        sum = 0;
        total = 0;
        for (Double d : vectory){
            if (d!= null && d!= 0){
                sum += (d*d);
                total ++;
            }
        }
        if (sum == 0 || total == 0)
            return null;

        s_y = Math.sqrt(sum/total);

        double x_i, y_i;
        total = 0;
        for (int count = 0; count < vectorx.size(); count ++){
            if (vectorx.get(count) != null && vectory.get(count) != null){
                x_i = vectorx.get(count);
                y_i = vectory.get(count);
                if (x_i != 0 && y_i != 0){
                    s_xy += (x_i/s_x) * (y_i/s_y);
                    total++;
                }
            }
        }
        // can abs val too
        if (total == 0){
            return null;
        }
        // To get the distance
        return 1 - s_xy/total;
    }

    /**
     * This function searches the distance matrix to find the pair with the 
     * shortest distance between them. The indices of the pair are returned 
     * in ip and jp; the distance itself is returned by the function.
     * @param n
     * @param distmatrix
     * @return 
     */
    static Integer[] find_closest_pair(int n, Vector<Vector<Double>> distmatrix){
	  Double temp;
	  Double dist = null;
	  Integer[] vals = new Integer[2]; // i, j
	  Integer is = null, js = null;
	  for (int j = 0; j < n; j++){ 
              for (int i = 0; i < j; i++){ 
                  temp = distmatrix.get(j).get(i);
                  // MAKE SURE I AND J ARE NOT EQUAL (if using the same query and array parts)
                  if (temp != null){
                      if (dist == null || temp < dist){ 
                          dist = temp;
                          is = i;
                          js = j;
                      }
                  }
              }
	  }
	  vals[0] = is; //is
	  vals[1] = js; //js
	  return vals;
	}
}
