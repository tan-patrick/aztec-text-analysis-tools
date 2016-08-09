/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ucla.cs.scai.aztec.similarity;

import edu.ucla.cs.scai.aztec.summarization.RankedString;
import edu.ucla.cs.scai.aztec.textexpansion.TextExpansion;
import edu.ucla.cs.scai.aztec.textexpansion.TextParser;
import edu.ucla.cs.scai.aztec.AztecEntry;
import edu.ucla.cs.scai.aztec.dto.SearchResultPage;
import net.sf.extjwnl.JWNLException;

import java.util.*;

/**
 *
 * @author Giuseppe M. Mazzeo <mazzeo@cs.ucla.edu>
 */
public class Search {

    //Tokenizer tokenizer;
    TextParser textparser;
    TextExpansion textexpansion;

    public Search() throws Exception {
        textparser = new TextParser();
        textexpansion = new TextExpansion();
    }

    public SearchResultPage searchQueryWithOnlyKeywordsTFIDF(String query, Integer offset, Integer limit) {
        if (offset == null) {
            offset = 0;
        }
        if (limit == null) {
            limit = Integer.MAX_VALUE;
        }
        ArrayList<WeightedEntry> res = new ArrayList<>();
        try {
            //Tokenizer t = new Tokenizer();
            TextParser tp = new TextParser();
            TextExpansion te = new TextExpansion();
            //LinkedList<String> tokens = t.tokenize(query);
            LinkedList<String> origintokens = tp.queryParser(query);
            LinkedList<RankedString> tokens  = te.queryExpansion(origintokens);
            if (!tokens.isEmpty()) {
                for (String entry : CachedData.tfidtK.keySet()) {
                    HashMap<String, Double> row2 = CachedData.tfidtK.get(entry);
                    double product = 0;
                    for (RankedString w : tokens) {
                        Double val = row2.get(w.getString());
                        if (val != null) {
                            product += val*w.getRank(); // suppose the query has already been normalized
                        }
                    }
                    double sim = product;// / length2;
                    if (Double.isFinite(sim) && sim > 0) {
                        res.add(new WeightedEntry(CachedData.entryMap.get(entry), sim));
                    }
                }
            }
        } catch (Exception e) {
        }

        Collections.sort(res);
        ArrayList<AztecEntry> resk = new ArrayList<>();
        for (int i = offset; i < Math.min(res.size(), limit + offset); i++) {
            WeightedEntry we = res.get(i);
            resk.add(we.entry);
        }
        return new SearchResultPage(resk, res.size());
    }

    public ArrayList<WeightedEntry> getMostSimilarEntriesToQuery(String qs, int k) throws Exception{

        ArrayList<WeightedEntry> res = new ArrayList<>();
        LinkedList<String> origintokens = new LinkedList<>();
        origintokens = textparser.queryParser(qs);
        LinkedList<RankedString> tokens = textexpansion.queryExpansion(origintokens);
        HashMap<String, Double> wordCount = new HashMap<>();
        int max = 1;
        for (RankedString w : tokens) {
            //now calculating tf for words in query;
//            Integer c = wordCount.get(w.getString());
//            if (c == null) {
//                wordCount.put(w.getString(), 1);
//            } else {
//                wordCount.put(w.getString(), c + 1);
//                max = Math.max(max, c + 1);
//            }
//            Double c = wordCount.get(w.getString());
//            if( c == null){
//                wordCount.put(w.getString(),w.getRank());
//            }
//            else{
//                Double pre = wordCount.get(w.getString());
//                Double max_score = Math.max(pre,c);
//                wordCount.put(w.getString(),max_score);
//            }
            Double c = wordCount.get(w.getString());
            if (c == null) {
                wordCount.put(w.getString(), w.getRank());
            } else {
                wordCount.put(w.getString(), c + w.getRank());
            }
        }
        HashMap<String, Double> queryTfidt = new HashMap<>();
        double queryLength = 0;
        for (String w : wordCount.keySet()) {
            Double val = CachedData.idfK.get(w); ////////////////////
            if (val != null) {
                //val *= 1.0 * wordCount.get(w) / max;
                // the calculation methods have problem, log(w) will get to 0
                val *= 1 + Math.log(wordCount.get(w)) / Math.log(2);
                queryLength += val * val;
                queryTfidt.put(w, val);
            }
        }
        queryLength = Math.sqrt(queryLength);
        for (String entry : CachedData.tfidtK.keySet()) {
            double docLength = CachedData.documentLengthK.get(entry);
            HashMap<String, Double> row = CachedData.tfidtK.get(entry);
            double sim = 0;
            for (String w : wordCount.keySet()) {
                Double val = row.get(w);
                if (val != null) {
                    sim += val * queryTfidt.get(w);
                }
            }
            sim /= (queryLength * docLength); // calculate cos similarity
            if (sim > 0) {
                res.add(new WeightedEntry(CachedData.entryMap.get(entry), sim));
            }
        }
        Collections.sort(res);


        return res;
    }
    public static void main(String[] args) throws Exception{
        Search handle = new Search();
        String query = "cluster gene";
        ArrayList<WeightedEntry> res = handle.getMostSimilarEntriesToQuery(query,10);
        for (WeightedEntry r:res){
            System.out.print(r.weight);
            System.out.println(r.entry.getDescription());
        }
    }

}
