/*
 *  Copyright 2002-2017 Barcelona Supercomputing Center (www.bsc.es)
 *  Life Science Department, 
 *  Computational Genomics Group (http://www.bsc.es/life-sciences/computational-genomics)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *
 *  Last update: $LastChangedDate: 2017-14-08 11:36:54 +0100 (Mon, 14 Ago 2017) $
 *  Revision Number: $Revision: 16 $
 *  Last revision  : $LastChangedRevision: 16 $
 *  Written by     : Friman Sanchez C.
 *                 : friman.sanchez@gmail.com
 *  Modified by    : COMPSs Support
 *                 : support-compss@bsc.es
 *                
 *  Guidance web page: http://cg.bsc.es/guidance/
 *
 */

package guidance;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Date;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import guidance.GuidanceImpl;
import guidance.exceptions.GuidanceEnvironmentException;
import guidance.exceptions.GuidanceTaskException;
import guidance.files.AssocFiles;
import guidance.files.CombinedPanelsFiles;
import guidance.files.CommonFiles;
import guidance.files.FileUtils;
import guidance.files.ImputationFiles;
import guidance.files.MergeFiles;
import guidance.files.PhenomeAnalysisFiles;
import guidance.files.ResultsFiles;
import guidance.utils.ChromoInfo;
import guidance.utils.ParseCmdLine;


/**
 * The higher-level class of Guidance
 */
public class Guidance {

    // Package version
    private static final String PACKAGE_VERSION = "guidance_0.9.8";

    // Debug mode
    private static final boolean DEBUG = false;

    // Threshold
    private static final double PVA_THRESHOLD = 5e-8;
    private static final String PVA_THRESHOLD_STR = Double.toString(PVA_THRESHOLD);

    // Environment variables that contain the information of the applications that will be used in the Guidance
    // execution
    private static final String PLINK_BINARY = System.getenv("PLINKBINARY");
    private static final String GTOOL_BINARY = System.getenv("GTOOLBINARY");
    private static final String R_SCRIPT_BIN_DIR = System.getenv("RSCRIPTBINDIR");
    private static final String R_SCRIPT_DIR = System.getenv("RSCRIPTDIR");
    private static final String QCTOOL_BINARY = System.getenv("QCTOOLBINARY");
    private static final String SHAPEIT_BINARY = System.getenv("SHAPEITBINARY");
    private static final String IMPUTE2_BINARY = System.getenv("IMPUTE2BINARY");
    // private static final String MINIMAC_BINARY = System.getenv("MINIMACBINARY");
    private static final String SNPTEST_BINARY = System.getenv("SNPTESTBINARY");
    private static final String JAVA_HOME = System.getenv("JAVA_HOME");


    /**
     * GUIDANCE main code
     * 
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        ArrayList<String> listOfCommands = new ArrayList<>();

        // Verify that all environment variables have been defined correctly
        verifyEnvVar();

        // Print information of Guidance version
        printGuidancePackageVersion();
        if (DEBUG) {
            printEnVariables();
        }

        // Get the input arguments
        ParseCmdLine parsingArgs = new ParseCmdLine(args);

        // Verify and print the status of each stage
        printStagesStatus(parsingArgs);
        System.out.println("\n[Guidance] Verifyed stages status.");

        // Get the file name where the list of commands is going to be saved (listOfStagesFile)
        String listOfStagesFileName = parsingArgs.getListOfStagesFile();

        // Verify whether the file exists or not.
        File listOfStages = new File(listOfStagesFileName);
        if (!listOfStages.exists()) {
            System.out.println("\n[Guidance] File to store the tasks list: " + listOfStagesFileName);
        } else {
            System.out.println("\n[Guidance] File to store the tasks list (overwritten): " + listOfStagesFileName);
        }
        if (!listOfStages.createNewFile()) {
            System.err.println("[Guidance] Error on main cannot create list of stages file: " + listOfStages);
        }

        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        String datestring = dateFormat.format(date);

        listOfCommands.add("####################################################################");
        listOfCommands.add("# List of tasks executed by Guidance workflow");
        listOfCommands.add("# Date: " + datestring);
        listOfCommands.add("# Parameters of the execution: ");
        listOfCommands.add("####################################################################");

        // Get the names of the reference panel to be used in the execution
        ArrayList<String> rpanelTypes = new ArrayList<String>(parsingArgs.getRpanelTypes());
        String outDir = parsingArgs.getOutDir();
        ChromoInfo generalChromoInfo = new ChromoInfo();

        System.out.println("[Guidance] We print testTypes information");
        int numberOfTestTypes = parsingArgs.getNumberOfTestTypeName();
        for (int kk = 0; kk < numberOfTestTypes; kk++) {
            String tmp_test_type = parsingArgs.getTestTypeName(kk);
            String tmp_responseVar = parsingArgs.getResponseVar(kk);
            String tmp_covariables = parsingArgs.getCovariables(kk);
            System.out.println("[Guidance] " + tmp_test_type + " = " + tmp_responseVar + ":" + tmp_covariables);
        }

        // Main code of the work flow:
        try {
            // Now, we have to ask whether the GWAS process is mixed.
            // printEnVariables();
            doMixed(parsingArgs, outDir, rpanelTypes, generalChromoInfo, listOfCommands);
        } catch (Exception e) {
            System.err.println("[Guidance] Error on main GWAS workflow process.");
            System.err.println(e.getMessage());
        }

        // Finally, we print the commands in the output file defined for this.
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(listOfStages))) {
            for (String str : listOfCommands) {
                writer.write(str);
                writer.newLine();
                writer.newLine();
            }

            // Close the file with the list of commands...
            writer.flush();
        }
        System.out.println("[Guidance] Everything is working with Guidance, just wait...");

    }

    /**
     * Method to print a all the environment variables of the system. It is only used for debug
     */
    private static void printEnVariables() {
        long freeMemory = Runtime.getRuntime().freeMemory() / 1_048_576;
        long totalMemory = Runtime.getRuntime().totalMemory() / 1_048_576;
        long maxMemory = Runtime.getRuntime().maxMemory() / 1_048_576;

        System.out.println("JVM freeMemory: " + freeMemory);
        System.out.println("JVM totalMemory also equals to initial heap size of JVM : " + totalMemory);
        System.out.println("JVM maxMemory also equals to maximum heap size of JVM   : " + maxMemory);

        Map<String, String> env = System.getenv();
        System.out.println("--------------------------------------");
        System.out.println("Environmental Variables in Master:");
        for (String envName : env.keySet()) {
            System.out.format("%s=%s%n", envName, env.get(envName));
        }
        System.out.println("--------------------------------------");
        ArrayList<String> objects = new ArrayList<>();
        for (int ii = 0; ii < 10000000; ii++) {
            objects.add(("" + 10 * 2710));
        }

        freeMemory = Runtime.getRuntime().freeMemory() / 1_048_576;
        totalMemory = Runtime.getRuntime().totalMemory() / 1_048_576;
        maxMemory = Runtime.getRuntime().maxMemory() / 1_048_576;

        System.out.println("Used Memory in JVM: " + (maxMemory - freeMemory));
        System.out.println("freeMemory in JVM: " + freeMemory);
        System.out.println("totalMemory in JVM shows current size of java heap : " + totalMemory);
        System.out.println("maxMemory in JVM: " + maxMemory);
    }

    /**
     * Method that performs the complete work flow when "mixed" type of GWAS is chosen.
     * 
     * @param parsingArgs
     * @param outDir
     * @param rpanelTypes
     * @param generalChromoInfo
     * @param listOfCommands
     */
    private static void doMixed(ParseCmdLine parsingArgs, String outDir, List<String> rpanelTypes, ChromoInfo generalChromoInfo,
            ArrayList<String> listOfCommands) {

        // Create some general objects
        int startChr = parsingArgs.getStart();
        int endChr = parsingArgs.getEnd();
        // int endChrNormal = parsingArgs.getEndNormal();

        String exclCgatFlag = parsingArgs.getExclCgatSnp();
        String exclSVFlag = parsingArgs.getExclSVSnp();

        // double mafThreshold = parsingArgs.getMafThreshold();
        // double infoThreshold = parsingArgs.getInfoThreshold();
        // double hweCohortThreshold = parsingArgs.getHweCohortThreshold();
        // double hweCasesThreshold = parsingArgs.getHweCasesThreshold();
        // double hweControlsThreshold = parsingArgs.getHweControlsThreshold();

        // String mafThresholdS = Double.toString(mafThreshold);
        // String infoThresholdS = Double.toString(infoThreshold);
        // String hweCohortThresholdS = Double.toString(hweCohortThreshold);
        // String hweCasesThresholdS = Double.toString(hweCasesThreshold);
        // String hweControlsThresholdS = Double.toString(hweControlsThreshold);

        int chunkSize = parsingArgs.getChunkSize();

        String inputFormat = null;

        // Create the names for the common files
        CommonFiles commonFilesInfo = new CommonFiles(parsingArgs, outDir);

        // Create the whole directory structure.
        System.out.println("[Guidance] Creating the directory structures for the outputs...");
        FileUtils.createDirStructure(parsingArgs, outDir, rpanelTypes, startChr, endChr);

        // Create the names for mixed files
        ImputationFiles imputationFilesInfo = new ImputationFiles(parsingArgs, generalChromoInfo, outDir, rpanelTypes);

        // Create the names for Association files
        AssocFiles assocFilesInfo = new AssocFiles(parsingArgs, generalChromoInfo, outDir, rpanelTypes);

        // Create the names for Merge files
        MergeFiles mergeFilesInfo = new MergeFiles(parsingArgs, generalChromoInfo, outDir, rpanelTypes);

        // Create the names for Results Files. Take into account this class it to generate file name for results
        // of each testType and each rPanelType. The results for combined panels or phenome analysis are created
        // by other class.
        ResultsFiles resultsFilesInfo = new ResultsFiles(parsingArgs, outDir, rpanelTypes);

        CombinedPanelsFiles combinedPanelsFilesInfo = new CombinedPanelsFiles(parsingArgs, outDir, rpanelTypes);

        PhenomeAnalysisFiles phenomeAnalysisFilesInfo = new PhenomeAnalysisFiles(parsingArgs, outDir, rpanelTypes);

        // Create all file names used in the workflow.
        // Also, define which of them will be temporal or permanent.
        setFinalStatusForCommonFiles(parsingArgs, commonFilesInfo);
        setFinalStatusForImputationFiles(parsingArgs, imputationFilesInfo, generalChromoInfo, rpanelTypes);
        setFinalStatusForAssocFiles(parsingArgs, assocFilesInfo, generalChromoInfo, rpanelTypes);

        // int numOfChromosToProcess = endChr - startChr + 1;
        for (int i = startChr; i <= endChr; i++) {
            // We get the output pairs file name for mixed
            String mixedPairsFile = commonFilesInfo.getPairsFile(i);
            String mixedSampleFile = commonFilesInfo.getSampleFile(i);

            String theChromo = Integer.toString(i);
            String mixedGenFile = null;
            String bedFile = null;
            String bimFile = null;
            String famFile = null;
            String mixedBedFile = null;
            String mixedBimFile = null;
            String mixedFamFile = null;
            String mixedBedToBedLogFile = null;

            String gmapFile = parsingArgs.getGmapDir() + "/" + parsingArgs.getGmapFileName(i);
            String mixedShapeitHapsFile = commonFilesInfo.getShapeitHapsFile(i);
            String mixedShapeitSampleFile = commonFilesInfo.getShapeitSampleFile(i);
            String mixedShapeitLogFile = commonFilesInfo.getShapeitLogFile(i);
            String mixedExcludedSnpsFile = commonFilesInfo.getExcludedSnpsFile(i);

            String mixedFilteredHaplotypesFile = commonFilesInfo.getFilteredHaplotypesFile(i);
            String mixedFilteredHaplotypesSampleFile = commonFilesInfo.getFilteredHaplotypesSampleFile(i);
            String mixedFilteredHaplotypesLogFile = commonFilesInfo.getFilteredHaplotypesLogFile(i);
            String mixedFilteredHaplotypesVcfFile = commonFilesInfo.getFilteredHaplotypesVcfFile(i);
            String mixedFilteredListOfSnpsFile = commonFilesInfo.getListOfSnpsFile(i);

            // Check if the input are in GEN o BED format
            inputFormat = parsingArgs.getInputFormat();
            if (inputFormat.equals("BED")) {
                bedFile = commonFilesInfo.getBedFile();
                bimFile = commonFilesInfo.getBimFile();
                famFile = commonFilesInfo.getFamFile();

                mixedBedFile = commonFilesInfo.getByChrBedFile(i);
                mixedBimFile = commonFilesInfo.getByChrBimFile(i);
                mixedFamFile = commonFilesInfo.getByChrFamFile(i);
                mixedBedToBedLogFile = commonFilesInfo.getBedToBedLogFile(i);

                doConvertFromBedToBed(parsingArgs, listOfCommands, bedFile, bimFile, famFile, mixedBedFile, mixedBimFile, mixedFamFile,
                        mixedBedToBedLogFile, theChromo);

                // Create the RsId list of SNPs that are AT, TA, CG, or GC
                // In that case, because inputType is BED we pass the newBimFile
                doCreateRsIdList(parsingArgs, listOfCommands, mixedBimFile, exclCgatFlag, mixedPairsFile, inputFormat);

                doPhasingBed(parsingArgs, listOfCommands, theChromo, mixedBedFile, mixedBimFile, mixedFamFile, gmapFile,
                        mixedExcludedSnpsFile, mixedShapeitHapsFile, mixedShapeitSampleFile, mixedShapeitLogFile,
                        mixedFilteredHaplotypesFile, mixedFilteredHaplotypesSampleFile, mixedFilteredHaplotypesLogFile,
                        mixedFilteredHaplotypesVcfFile, mixedFilteredListOfSnpsFile, exclCgatFlag, exclSVFlag);

            } else { // The inputFormat is GEN
                mixedGenFile = commonFilesInfo.getGenFile(i);
                // Task
                doCreateRsIdList(parsingArgs, listOfCommands, mixedGenFile, exclCgatFlag, mixedPairsFile, inputFormat);

                doPhasing(parsingArgs, listOfCommands, theChromo, mixedGenFile, mixedSampleFile, gmapFile, mixedExcludedSnpsFile,
                        mixedShapeitHapsFile, mixedShapeitSampleFile, mixedShapeitLogFile, mixedFilteredHaplotypesFile,
                        mixedFilteredHaplotypesSampleFile, mixedFilteredHaplotypesLogFile, mixedFilteredHaplotypesVcfFile,
                        mixedFilteredListOfSnpsFile, exclCgatFlag, exclSVFlag);

            } // End of inputFormat.equals("GEN")

            // ***********************************************
            // * Now perform the imputation tasks *
            // * over the data for the different reference *
            // * panels that the user wants to include. The *
            // * list of the panels to be used are stored in *
            // * the List array rpanelTypes *
            // ***********************************************
            for (int kk = 0; kk < rpanelTypes.size(); kk++) {
                // String rpanelDir = parsingArgs.getRpanelDir(kk);

                int maxSize = generalChromoInfo.getMaxSize(i);
                int minSize = generalChromoInfo.getMinSize(i);
                int lim1 = minSize;
                int lim2 = lim1 + chunkSize - 1;
                int j = 0;
                for (j = minSize; j < maxSize; j = j + chunkSize) {
                    makeImputationPerChunk(parsingArgs, i, lim1, lim2, kk, gmapFile, imputationFilesInfo, commonFilesInfo, listOfCommands);
                    lim1 = lim1 + chunkSize;
                    lim2 = lim2 + chunkSize;
                }
            } // End for(kk=0; kk<rpanelTypes.size(); kk++)

        } // End for(i=startChr;i <= endChr; i++)

        // *******************************************************************
        // * COMPSs API Call to wait for all tasks *
        // * comment out following line to include the synchronization point *
        // *******************************************************************
        // COMPSs.waitForAllTasks();

        // Now we continue with the association
        int numberOfTestTypes = parsingArgs.getNumberOfTestTypeName();
        for (int tt = 0; tt < numberOfTestTypes; tt++) {
            for (int kk = 0; kk < rpanelTypes.size(); kk++) {
                for (int i = startChr; i <= endChr; i++) {
                    int maxSize = generalChromoInfo.getMaxSize(i);
                    int minSize = generalChromoInfo.getMinSize(i);
                    int lim1 = minSize;
                    int lim2 = lim1 + chunkSize - 1;
                    /*
                     * for(j=minSize; j<maxSize; j = j + chunkSize) { makeAssociationPerChunk(parsingArgs,tt, kk, i,
                     * lim1, lim2, imputationFilesInfo, commonFilesInfo, assocFilesInfo, listOfCommands);
                     * 
                     * lim1 = lim1 + chunkSize; lim2 = lim2 + chunkSize; }
                     * 
                     * // Now we perform the merge of chunks for each chromosome makeMergeOfChunks(parsingArgs,
                     * listOfCommands, tt, kk, i, minSize, maxSize, chunkSize, assocFilesInfo, mergeFilesInfo);
                     * 
                     * // Then, we have to filterByAll the last merged File of each chromomose. String
                     * theLastReducedFile = mergeFilesInfo.getTheLastReducedFile(tt, kk, i); String filteredByAllFile =
                     * mergeFilesInfo.getFilteredByAllFile(tt, kk, i); String condensedFile =
                     * mergeFilesInfo.getCondensedFile(tt, kk,i);
                     * 
                     * doFilterByAll(parsingArgs, listOfCommands, theLastReducedFile, filteredByAllFile, condensedFile);
                     */

                    for (int j = minSize; j < maxSize; j = j + chunkSize) {

                        makeAssociationPerChunk(parsingArgs, tt, kk, i, lim1, lim2, imputationFilesInfo, commonFilesInfo, assocFilesInfo,
                                listOfCommands);

                        lim1 = lim1 + chunkSize;
                        lim2 = lim2 + chunkSize;
                    }

                    // Now we perform the merge of chunks for each chromosome
                    String filtered = "filtered";
                    String condensed = "condensed";
                    makeMergeOfChunks(parsingArgs, listOfCommands, tt, kk, i, minSize, maxSize, chunkSize, assocFilesInfo, mergeFilesInfo,
                            filtered);
                    makeMergeOfChunks(parsingArgs, listOfCommands, tt, kk, i, minSize, maxSize, chunkSize, assocFilesInfo, mergeFilesInfo,
                            condensed);
                } // End for Chromo

                // Now we have to joint the condensedFiles of each chromosome. There is not problem if chr23 is being
                // processed, because
                // the format of condensedFiles is the same for all chromosome.
                makeJointCondensedFiles(parsingArgs, listOfCommands, tt, kk, startChr, endChr, mergeFilesInfo);

                // Now we have to joint the filteredByAllFiles of each chromosome. Here there is an additional
                // complexity due to the chr23,
                // because the file format changes if chr23 is being processed. This situation is taken into account
                // inside the next function.
                String rpanelName = rpanelTypes.get(kk);

                makeJointFilteredByAllFiles(parsingArgs, listOfCommands, tt, kk, rpanelName, startChr, endChr, mergeFilesInfo);

                String lastCondensedFile = mergeFilesInfo.getFinalCondensedFile(tt, kk);
                // System.out.println("\n[Guidance] The last Condensed file is " + lastCondensedFile);

                String lastFilteredByAllFile = mergeFilesInfo.getFinalFilteredByAllFile(tt, kk);
                // System.out.println("\n[Guidance] The last FilteredByAll file is " + lastFilteredByAllFile);

                // Lets extract top-hits results from the lastFilteredByAllFile
                String topHitsResults = resultsFilesInfo.getTopHitsFile(tt, kk);
                // System.out.println("\n[Guidance] The topHitsFile is " + topHitsResults);

                String filteredByAllXFile = null;
                if (endChr == 23) {
                    filteredByAllXFile = mergeFilesInfo.getAdditionalFilteredByAllXFile(tt, kk, 0);
                } else {
                    filteredByAllXFile = lastFilteredByAllFile;
                }

                doGenerateTopHits(parsingArgs, listOfCommands, lastFilteredByAllFile, filteredByAllXFile, topHitsResults,
                        PVA_THRESHOLD_STR);

                String qqPlotPdfFile = resultsFilesInfo.getQqPlotPdfFile(tt, kk);
                String qqPlotTiffFile = resultsFilesInfo.getQqPlotTiffFile(tt, kk);

                String manhattanPlotPdfFile = resultsFilesInfo.getManhattanPdfFile(tt, kk);
                String manhattanPlotTiffFile = resultsFilesInfo.getManhattanTiffFile(tt, kk);

                String correctedPvaluesFile = resultsFilesInfo.getCorrectedPvaluesFile(tt, kk);

                doGenerateQQManhattanPlots(parsingArgs, listOfCommands, lastCondensedFile, qqPlotPdfFile, manhattanPlotPdfFile,
                        qqPlotTiffFile, manhattanPlotTiffFile, correctedPvaluesFile);

            } // End for refPanels

            // Now we continue with the combining of the results of the different reference panels.
            // It is done if the refPanelCombine flag is true.
            makeCombinePanels(parsingArgs, assocFilesInfo, mergeFilesInfo, combinedPanelsFilesInfo, rpanelTypes, tt, listOfCommands,
                    generalChromoInfo);

        } // End for test types

        if (1 < numberOfTestTypes) {
            makePhenotypeAnalysis(parsingArgs, mergeFilesInfo, resultsFilesInfo, phenomeAnalysisFilesInfo, rpanelTypes, listOfCommands);
        } else {
            System.out.println("\n[Guidance] No cross-phenotype analysis. Only one phenotype available");
        }

        // makePhenotypeAnalysis(parsingArgs, mergeFilesInfo, resultsFilesInfo, phenomeAnalysisFilesInfo, rpanelTypes,
        // listOfCommands);

        // -------------------------->
        // makeMergingAndSummaryResults(parsingArgs, rpanelTypes, assocFilesInfo, startChr, endChr, listOfCommands);

        // makeCombinePanels(parsingArgs, rpanelTypes, assocFilesInfo, numOfChromosToProcess,
        // refPanelCombine, mafThreshold, infoThreshold, hweCohortThreshold,
        // hweCasesThreshold, hweControlsThreshold, listOfCommands);

        // Final part: Summary, qqplot and Manhatan plots generation.
        /*
         * makeMergingAndDrawingResults(parsingArgs, rpanelTypes, assocFilesInfo, numOfChromosToProcess,
         * refPanelCombine, mafThreshold, infoThreshold, hweCohortThreshold, hweCasesThreshold, hweControlsThreshold,
         * startChr, endChr, listOfCommands);
         */
        System.out.println("\n[Guidance] All tasks are in execution, please wait...");

        /**
         * Now it is a good moment to start with the cleaning and compression of the temporal files. It should be done
         * if variable removeTemporalFiles and compressFiles where enabled. We compress and clean in this order: -
         * commonFilesInfo - imputationFilesInfo - assocFilesInfo Due to that currently COMPSs does not allow us to
         * delete de temporal files, then we will compress them and then we create a text file that list the files that
         * we want to clean. After the end of the execution, the user can delete them.
         */
        /*
         * try{ compressCommonFiles(parsingArgs, commonFilesInfo); } catch (Exception e){
         * System.err.println("[Guidance] Exception compressing commonFilesInfo."); }
         * 
         * try{ compressImputationFiles(parsingArgs, generalChromoInfo, rpanelTypes, imputationFilesInfo); } catch
         * (Exception e){ System.err.println("[Guidance] Exception compressing imputationFilesInfo."); }
         * 
         * try{ compressAssocFiles(parsingArgs, generalChromoInfo, rpanelTypes, assocFilesInfo); } catch (Exception e){
         * System.err.println("[Guidance] Exception compressing imputationFilesInfo."); }
         * 
         * // Now we delete files try{ deleteCommonFiles(parsingArgs, commonFilesInfo); } catch (Exception e){
         * System.err.println("[Guidance] Exception deleting commonFilesInfo."); }
         * 
         * try{ deleteImputationFiles(parsingArgs, generalChromoInfo, rpanelTypes, imputationFilesInfo); } catch
         * (Exception e){ System.err.println("[Guidance] Exception deleting imputationFilesInfo."); }
         * 
         * try{ deleteAssocFiles(parsingArgs, generalChromoInfo, rpanelTypes, assocFilesInfo); } catch (Exception e){
         * System.err.println("[Guidance] Exception deleting imputationFilesInfo."); }
         */
    }

    /**
     * Method that generates all the tasks for imputation and association.
     * 
     * @param parsingArgs
     * @param chrNumber
     * @param lim1
     * @param lim2
     * @param panelIndex
     * @param gmapFile
     * @param imputationFilesInfo
     * @param commonFilesInfo
     * @param listOfCommands
     */
    private static void makeImputationPerChunk(ParseCmdLine parsingArgs, int chrNumber, int lim1, int lim2, int panelIndex, String gmapFile,
            ImputationFiles imputationFilesInfo, CommonFiles commonFilesInfo, ArrayList<String> listOfCommands) {

        int chunkSize = parsingArgs.getChunkSize();
        double infoThreshold = parsingArgs.getInfoThreshold();

        String knownHapFileName = parsingArgs.getRpanelHapFileName(panelIndex, chrNumber);
        String rpanelDir = parsingArgs.getRpanelDir(panelIndex);
        String knownHapFile = rpanelDir + "/" + knownHapFileName;

        String lim1S = Integer.toString(lim1);
        String lim2S = Integer.toString(lim2);
        String chrS = Integer.toString(chrNumber);
        String infoThresholdS = Double.toString(infoThreshold);
        String imputationTool = parsingArgs.getImputationTool();

        if (imputationTool.equals("impute")) {
            String legendFileName = parsingArgs.getRpanelLegFileName(panelIndex, chrNumber);
            String legendFile = rpanelDir + "/" + legendFileName;

            // String mixedSampleFile = commonFilesInfo.getSampleFile(chrNumber);
            String mixedShapeitHapsFile = commonFilesInfo.getShapeitHapsFile(chrNumber);
            String mixedShapeitSampleFile = commonFilesInfo.getShapeitSampleFile(chrNumber);
            String mixedPairsFile = commonFilesInfo.getPairsFile(chrNumber);
            String mixedImputeFile = imputationFilesInfo.getImputedFile(panelIndex, chrNumber, lim1, lim2, chunkSize);
            String mixedImputeFileInfo = imputationFilesInfo.getImputedInfoFile(panelIndex, chrNumber, lim1, lim2, chunkSize);
            String mixedImputeFileSummary = imputationFilesInfo.getImputedSummaryFile(panelIndex, chrNumber, lim1, lim2, chunkSize);
            String mixedImputeFileWarnings = imputationFilesInfo.getImputedWarningsFile(panelIndex, chrNumber, lim1, lim2, chunkSize);
            // String mixedImputeLogFile = imputationFilesInfo.getImputedLogFile(panelIndex, chrNumber, lim1, lim2,
            // chunkSize);

            String mixedFilteredFile = imputationFilesInfo.getFilteredFile(panelIndex, chrNumber, lim1, lim2, chunkSize);
            String mixedFilteredLogFile = imputationFilesInfo.getFilteredLogFile(panelIndex, chrNumber, lim1, lim2, chunkSize);

            // We create the list of rsId that are greater than or equal to the infoThreshold value
            String mixedFilteredRsIdFile = imputationFilesInfo.getFilteredRsIdFile(panelIndex, chrNumber, lim1, lim2, chunkSize);

            doImputationWithImpute(parsingArgs, listOfCommands, chrS, gmapFile, knownHapFile, legendFile, mixedShapeitHapsFile,
                    mixedShapeitSampleFile, lim1S, lim2S, mixedPairsFile, mixedImputeFile, mixedImputeFileInfo, mixedImputeFileSummary,
                    mixedImputeFileWarnings);

            doFilterByInfo(parsingArgs, listOfCommands, mixedImputeFileInfo, mixedFilteredRsIdFile, infoThresholdS);

            doQctoolS(parsingArgs, listOfCommands, mixedImputeFile, mixedFilteredRsIdFile, mixedFilteredFile, mixedFilteredLogFile);
        } else if (imputationTool.equals("minimac")) {
            String mixedFilteredHaplotypesFile = commonFilesInfo.getFilteredHaplotypesFile(chrNumber);
            String mixedFilteredHaplotypesSampleFile = commonFilesInfo.getFilteredHaplotypesSampleFile(chrNumber);
            String mixedFilteredListOfSnpsFile = commonFilesInfo.getListOfSnpsFile(chrNumber);

            String mixedImputedMMFileName = imputationFilesInfo.getImputedMMFile(panelIndex, chrNumber, lim1, lim2, chunkSize);
            String mixedImputedMMInfoFile = imputationFilesInfo.getImputedMMInfoFile(panelIndex, chrNumber, lim1, lim2, chunkSize);
            // String mixedImputedMMDraftFile = imputationFilesInfo.getImputedMMDraftFile(panelIndex, chrNumber, lim1,
            // lim2, chunkSize);
            String mixedImputedMMErateFile = imputationFilesInfo.getImputedMMErateFile(panelIndex, chrNumber, lim1, lim2, chunkSize);
            String mixedImputedMMRecFile = imputationFilesInfo.getImputedMMRecFile(panelIndex, chrNumber, lim1, lim2, chunkSize);
            String mixedImputedMMDoseFile = imputationFilesInfo.getImputedMMDoseFile(panelIndex, chrNumber, lim1, lim2, chunkSize);
            String mixedImputedMMLogFile = imputationFilesInfo.getImputedMMLogFile(panelIndex, chrNumber, lim1, lim2, chunkSize);

            doImputationWithMinimac(parsingArgs, listOfCommands, knownHapFile, mixedFilteredHaplotypesFile,
                    mixedFilteredHaplotypesSampleFile, mixedFilteredListOfSnpsFile, mixedImputedMMFileName, mixedImputedMMInfoFile,
                    mixedImputedMMErateFile, mixedImputedMMRecFile, mixedImputedMMDoseFile, mixedImputedMMLogFile, chrS, lim1S, lim2S);
        } else {
            System.err.println("\t[makeImputationPerChunk]: Error, the imputation tool " + imputationTool + " is not allowed...");
            System.exit(1);
        }
    }

    /**
     * Method that generates all the tasks for association.
     * 
     * @param parsingArgs
     * @param testTypeIndex
     * @param panelIndex
     * @param chrNumber
     * @param lim1
     * @param lim2
     * @param imputationFilesInfo
     * @param commonFilesInfo
     * @param assocFilesInfo
     * @param listOfCommands
     */
    private static void makeAssociationPerChunk(ParseCmdLine parsingArgs, int testTypeIndex, int panelIndex, int chrNumber, int lim1,
            int lim2, ImputationFiles imputationFilesInfo, CommonFiles commonFilesInfo, AssocFiles assocFilesInfo,
            ArrayList<String> listOfCommands) {

        int chunkSize = parsingArgs.getChunkSize();
        double mafThreshold = parsingArgs.getMafThreshold();
        double infoThreshold = parsingArgs.getInfoThreshold();
        double hweCohortThreshold = parsingArgs.getHweCohortThreshold();
        double hweCasesThreshold = parsingArgs.getHweCasesThreshold();
        double hweControlsThreshold = parsingArgs.getHweControlsThreshold();

        String responseVar = parsingArgs.getResponseVar(testTypeIndex);
        String covariables = parsingArgs.getCovariables(testTypeIndex);

        String chrS = Integer.toString(chrNumber);

        String mafThresholdS = Double.toString(mafThreshold);
        String infoThresholdS = Double.toString(infoThreshold);
        String hweCohortThresholdS = Double.toString(hweCohortThreshold);
        String hweCasesThresholdS = Double.toString(hweCasesThreshold);
        String hweControlsThresholdS = Double.toString(hweControlsThreshold);

        String snptestOutFile = assocFilesInfo.getSnptestOutFile(testTypeIndex, panelIndex, chrNumber, lim1, lim2, chunkSize);
        String snptestLogFile = assocFilesInfo.getSnptestLogFile(testTypeIndex, panelIndex, chrNumber, lim1, lim2, chunkSize);

        String mixedSampleFile = commonFilesInfo.getSampleFile(chrNumber);
        String mixedImputeFileInfo = imputationFilesInfo.getImputedInfoFile(panelIndex, chrNumber, lim1, lim2, chunkSize);

        String mixedFilteredFile = imputationFilesInfo.getFilteredFile(panelIndex, chrNumber, lim1, lim2, chunkSize);

        // String inputFormat = parsingArgs.getInputFormat();
        // String exclCgatFlag = parsingArgs.getExclCgatSnp();

        doSnptest(parsingArgs, listOfCommands, chrS, mixedFilteredFile, mixedSampleFile, snptestOutFile, snptestLogFile, responseVar,
                covariables);

        String summaryFile = assocFilesInfo.getSummaryFile(testTypeIndex, panelIndex, chrNumber, lim1, lim2, chunkSize);

        doCollectSummary(parsingArgs, listOfCommands, chrS, mixedImputeFileInfo, snptestOutFile, summaryFile, mafThresholdS, infoThresholdS,
                hweCohortThresholdS, hweCasesThresholdS, hweControlsThresholdS);

        String assocFilteredByAll = assocFilesInfo.getSummaryFilteredFile(testTypeIndex, panelIndex, chrNumber, lim1, lim2, chunkSize);
        String assocCondensed = assocFilesInfo.getSummaryCondensedFile(testTypeIndex, panelIndex, chrNumber, lim1, lim2, chunkSize);

        doFilterByAll(parsingArgs, listOfCommands, summaryFile, assocFilteredByAll, assocCondensed);
    }

    /**
     * Method to perform the merging of chunks for each chromosome.
     * 
     * @param parsingArgs
     * @param listOfCommands
     * @param ttIndex
     * @param rpanelIndex
     * @param chr
     * @param minSize
     * @param maxSize
     * @param chunkSize
     * @param assocFilesInfo
     * @param mergeFilesInfo
     * @param type
     */
    private static void makeMergeOfChunks(ParseCmdLine parsingArgs, ArrayList<String> listOfCommands, int ttIndex, int rpanelIndex, int chr,
            int minSize, int maxSize, int chunkSize, AssocFiles assocFilesInfo, MergeFiles mergeFilesInfo, String type) {

        String theChromo = Integer.toString(chr);
        int lim1 = minSize;
        int lim2 = lim1 + chunkSize - 1;

        int numberOfChunks = maxSize / chunkSize;
        int module = maxSize % chunkSize;
        if (module != 0)
            numberOfChunks++;

        int indexA = 0;
        int indexC = 0;
        String reducedA = null;
        String reducedB = null;
        String reducedC = null;

        String filteredByAllFile = mergeFilesInfo.getFilteredByAllFile(ttIndex, rpanelIndex, chr);
        String condensedFile = mergeFilesInfo.getCondensedFile(ttIndex, rpanelIndex, chr);

        if (type == "filtered") {
            // System.out.println("Number of chunks for testType " + ttIndex + " | rpanel " + rpanelIndex + " |chr " +
            // chr + " " + numberOfChunks);
            for (int processedChunks = 0; processedChunks < 2 * numberOfChunks - 2; processedChunks = processedChunks + 2) {
                if (processedChunks < numberOfChunks) {
                    reducedA = assocFilesInfo.getSummaryFilteredFile(ttIndex, rpanelIndex, chr, lim1, lim2, chunkSize);
                    lim1 = lim1 + chunkSize;
                    lim2 = lim2 + chunkSize;
                } else {
                    reducedA = mergeFilesInfo.getReducedFilteredFile(ttIndex, rpanelIndex, chr, indexA);
                    indexA++;
                }
                if (processedChunks < numberOfChunks - 1) {
                    reducedB = assocFilesInfo.getSummaryFilteredFile(ttIndex, rpanelIndex, chr, lim1, lim2, chunkSize);
                    lim1 = lim1 + chunkSize;
                    lim2 = lim2 + chunkSize;
                } else {
                    reducedB = mergeFilesInfo.getReducedFilteredFile(ttIndex, rpanelIndex, chr, indexA);
                    indexA++;
                }

                if (processedChunks == 2 * numberOfChunks - 4) {
                    reducedC = mergeFilesInfo.getReducedFilteredFile(ttIndex, rpanelIndex, chr, indexC);
                    doMergeTwoChunks(parsingArgs, listOfCommands, reducedA, reducedB, filteredByAllFile, theChromo, type);
                    indexC++;
                } else {
                    reducedC = mergeFilesInfo.getReducedFilteredFile(ttIndex, rpanelIndex, chr, indexC);
                    doMergeTwoChunks(parsingArgs, listOfCommands, reducedA, reducedB, reducedC, theChromo, type);
                    indexC++;
                }

                /*
                 * File fA = new File(reducedA); fA.delete(); File fB = new File(reducedB); fB.delete();
                 */
            } // end for(processedChunks=0; processedChunks< numberOfChunks-1; processedChunks++)

        } else if (type == "condensed") {
            for (int processedChunks = 0; processedChunks < 2 * numberOfChunks - 2; processedChunks = processedChunks + 2) {
                if (processedChunks < numberOfChunks) {
                    reducedA = assocFilesInfo.getSummaryCondensedFile(ttIndex, rpanelIndex, chr, lim1, lim2, chunkSize);
                    lim1 = lim1 + chunkSize;
                    lim2 = lim2 + chunkSize;
                } else {
                    reducedA = mergeFilesInfo.getReducedCondensedFile(ttIndex, rpanelIndex, chr, indexA);
                    indexA++;
                }
                if (processedChunks < numberOfChunks - 1) {
                    reducedB = assocFilesInfo.getSummaryCondensedFile(ttIndex, rpanelIndex, chr, lim1, lim2, chunkSize);
                    lim1 = lim1 + chunkSize;
                    lim2 = lim2 + chunkSize;
                } else {
                    reducedB = mergeFilesInfo.getReducedCondensedFile(ttIndex, rpanelIndex, chr, indexA);
                    indexA++;
                }

                if (processedChunks == 2 * numberOfChunks - 4) {
                    reducedC = mergeFilesInfo.getReducedCondensedFile(ttIndex, rpanelIndex, chr, indexC);
                    doMergeTwoChunks(parsingArgs, listOfCommands, reducedA, reducedB, condensedFile, theChromo, type);
                    indexC++;
                } else {
                    reducedC = mergeFilesInfo.getReducedCondensedFile(ttIndex, rpanelIndex, chr, indexC);
                    doMergeTwoChunks(parsingArgs, listOfCommands, reducedA, reducedB, reducedC, theChromo, type);
                    indexC++;
                }

                /*
                 * File fA = new File(reducedA); fA.delete(); File fB = new File(reducedB); fB.delete();
                 */
            } // end for(processedChunks=0; processedChunks< numberOfChunks-1; processedChunks++)

        }

    }

    /**
     * Method to perform the joint of condensed files of each rpanel
     * 
     * @param parsingArgs
     * @param listOfCommands
     * @param ttIndex
     * @param rpanelIndex
     * @param startChr
     * @param endChr
     * @param mergeFilesInfo
     */
    private static void makeJointCondensedFiles(ParseCmdLine parsingArgs, ArrayList<String> listOfCommands, int ttIndex, int rpanelIndex,
            int startChr, int endChr, MergeFiles mergeFilesInfo) {

        int indexA = 0;
        int indexC = 0;
        int i = 0;
        String condensedA = null;
        String condensedB = null;
        String condensedC = null;
        int numberOfChrs = endChr - startChr + 1;

        if (numberOfChrs == 1) { // There is only one chr to process.
            // DO something.
            condensedA = mergeFilesInfo.getCondensedFile(ttIndex, rpanelIndex, startChr);
            condensedB = condensedA;

            condensedC = mergeFilesInfo.getAdditionalCondensedFile(ttIndex, rpanelIndex, indexC);
            doJointCondenseFiles(parsingArgs, listOfCommands, condensedA, condensedB, condensedC);
        } else {
            for (int processedCondensed = 0; processedCondensed < 2 * numberOfChrs - 2; processedCondensed = processedCondensed + 2) {
                if (processedCondensed < numberOfChrs) {
                    i = startChr + processedCondensed;
                    condensedA = mergeFilesInfo.getCondensedFile(ttIndex, rpanelIndex, i);
                } else {
                    condensedA = mergeFilesInfo.getAdditionalCondensedFile(ttIndex, rpanelIndex, indexA);
                    indexA++;
                }

                if (processedCondensed < numberOfChrs - 1) {
                    i = startChr + processedCondensed + 1;
                    condensedB = mergeFilesInfo.getCondensedFile(ttIndex, rpanelIndex, i);
                } else {
                    condensedB = mergeFilesInfo.getAdditionalCondensedFile(ttIndex, rpanelIndex, indexA);
                    indexA++;
                }
                condensedC = mergeFilesInfo.getAdditionalCondensedFile(ttIndex, rpanelIndex, indexC);

                doJointCondenseFiles(parsingArgs, listOfCommands, condensedA, condensedB, condensedC);
                indexC++;
            } // End for(int processedCondensed=0; processedCondensed<= 2*numberOfChrs -2; processedCondensed =
              // processedCondensed +2)
        }

        // File fA = new File(condensedA);
        // fA.delete();
        // File fB = new File(condensedB);
        // fB.delete();
    }

    /**
     * Method to perform the joint of filteredByAll files of each rpanel.
     * 
     * @param parsingArgs
     * @param listOfCommands
     * @param ttIndex
     * @param rpanelIndex
     * @param rpanelName
     * @param startChr
     * @param endChr
     * @param mergeFilesInfo
     */
    private static void makeJointFilteredByAllFiles(ParseCmdLine parsingArgs, ArrayList<String> listOfCommands, int ttIndex,
            int rpanelIndex, String rpanelName, int startChr, int endChr, MergeFiles mergeFilesInfo) {

        int endChrNormal = endChr;
        if (startChr < endChr) {
            if (endChr != 23) {
                endChrNormal = endChr;
            } else {
                endChrNormal = endChr - 1;
            }
        }

        int indexA = 0;
        int indexC = 0;
        int processedFiltered = 0;
        int i = 0;
        String rpanelFlag = "NO";
        String filteredA = null;
        String filteredB = null;
        String filteredC = null;
        int numberOfChrs = endChrNormal - startChr + 1;

        if (numberOfChrs == 1) { // There is only one chr to process.
            rpanelFlag = "YES";
            // DO something.
            filteredA = mergeFilesInfo.getFilteredByAllFile(ttIndex, rpanelIndex, startChr);
            filteredB = filteredA;

            filteredC = mergeFilesInfo.getAdditionalFilteredByAllFile(ttIndex, rpanelIndex, indexC);
            if (startChr != 23) {
                doJointFilteredByAllFiles(parsingArgs, listOfCommands, filteredA, filteredB, filteredC, rpanelName, rpanelFlag);
            }
        } else {
            for (processedFiltered = 0; processedFiltered < 2 * numberOfChrs - 2; processedFiltered = processedFiltered + 2) {
                if (processedFiltered < numberOfChrs) {
                    i = startChr + processedFiltered;
                    filteredA = mergeFilesInfo.getFilteredByAllFile(ttIndex, rpanelIndex, i);
                } else {
                    filteredA = mergeFilesInfo.getAdditionalFilteredByAllFile(ttIndex, rpanelIndex, indexA);
                    indexA++;
                }

                if (processedFiltered < numberOfChrs - 1) {
                    i = startChr + processedFiltered + 1;
                    filteredB = mergeFilesInfo.getFilteredByAllFile(ttIndex, rpanelIndex, i);
                } else {
                    filteredB = mergeFilesInfo.getAdditionalFilteredByAllFile(ttIndex, rpanelIndex, indexA);
                    indexA++;
                    rpanelFlag = "YES";
                }
                filteredC = mergeFilesInfo.getAdditionalFilteredByAllFile(ttIndex, rpanelIndex, indexC);

                doJointFilteredByAllFiles(parsingArgs, listOfCommands, filteredA, filteredB, filteredC, rpanelName, rpanelFlag);
                indexC++;
            } // End for(int processedFiltered=0; processedFiltered<= 2*numberOfChrs -2; processedFiltered =
              // processedFiltered +2)
        }

        // Now we process the chr 23 if this is defined in this execution
        if (endChr == 23) {
            rpanelFlag = "YES";
            filteredA = mergeFilesInfo.getFilteredByAllFile(ttIndex, rpanelIndex, endChr);
            filteredC = mergeFilesInfo.getAdditionalFilteredByAllXFile(ttIndex, rpanelIndex, 0);

            doJointFilteredByAllFiles(parsingArgs, listOfCommands, filteredA, filteredA, filteredC, rpanelName, rpanelFlag);
        }

        // File fA = new File(filteredA);
        // fA.delete();
        // File fB = new File(filteredB);
        // fB.delete();

    }

    /**
     * Method that performs the last part of the work flow corresponding to the merging, combining and drawing of
     * results
     * 
     * @param parsingArgs
     * @param assocFilesInfo
     * @param mergeFilesInfo
     * @param combinedPanelsFilesInfo
     * @param rpanelTypes
     * @param ttIndex
     * @param listOfCommands
     * @param generalChromoInfo
     */
    private static void makeCombinePanels(ParseCmdLine parsingArgs, AssocFiles assocFilesInfo, MergeFiles mergeFilesInfo,
            CombinedPanelsFiles combinedPanelsFilesInfo, List<String> rpanelTypes, int ttIndex, ArrayList<String> listOfCommands,
            ChromoInfo generalChromoInfo) {

        ArrayList<String> listDeleteFiles = new ArrayList<String>();

        int startChr = parsingArgs.getStart();
        int endChr = parsingArgs.getEnd();

        String filtered = "filtered";
        String condensed = "condensed";

        String filteredPanelA = null;
        String filteredPanelB = null;
        String filteredPanelC = null;

        String condensedPanelA = null;
        String condensedPanelB = null;
        String condensedPanelC = null;

        boolean refPanelCombine = parsingArgs.getRefPanelCombine();

        String filteredCombineA = null;
        String filteredCombineB = null;
        String filteredCombineC = null;

        String condensedCombineA = null;
        String condensedCombineB = null;
        String condensedCombineC = null;

        String filteredCombineXA = null;
        String filteredCombineXB = null;
        String filteredCombineXC = null;

        int chunkSize = parsingArgs.getChunkSize();

        // We have to ask if we have to combine results from different panels or not.
        if (refPanelCombine == true) {
            boolean elemA = false;
            for (int chr = startChr; chr <= endChr; chr++) {
                int maxSize = generalChromoInfo.getMaxSize(chr);
                int minSize = generalChromoInfo.getMinSize(chr);
                int lim1 = minSize;
                int lim2 = lim1 + chunkSize - 1;
                int indexFC = 0;
                int indexCC = 0;
                int indexXFC = 0;

                for (int j = minSize; j < maxSize; j = j + chunkSize) {
                    for (int kk = 1; kk < rpanelTypes.size(); kk++) {
                        if (kk == 1) {
                            filteredPanelA = assocFilesInfo.getSummaryFilteredFile(ttIndex, 0, chr, lim1, lim2, chunkSize);
                            condensedPanelA = assocFilesInfo.getSummaryCondensedFile(ttIndex, 0, chr, lim1, lim2, chunkSize);
                        }

                        // Filtered part
                        filteredPanelB = assocFilesInfo.getSummaryFilteredFile(ttIndex, kk, chr, lim1, lim2, chunkSize);
                        filteredPanelC = assocFilesInfo.getCombinedFilteredFile(ttIndex, kk - 1, chr, lim1, lim2, chunkSize);

                        doCombinePanelsComplex(parsingArgs, listOfCommands, filteredPanelA, filteredPanelB, filteredPanelC, lim1, lim2);
                        filteredPanelA = filteredPanelC;

                        // Condensed part
                        condensedPanelB = assocFilesInfo.getSummaryCondensedFile(ttIndex, kk, chr, lim1, lim2, chunkSize);
                        condensedPanelC = assocFilesInfo.getCombinedCondensedFile(ttIndex, kk - 1, chr, lim1, lim2, chunkSize);

                        doCombinePanelsComplex(parsingArgs, listOfCommands, condensedPanelA, condensedPanelB, condensedPanelC, lim1, lim2);
                        condensedPanelA = condensedPanelC;

                        listDeleteFiles.add(condensedPanelB);
                        listDeleteFiles.add(condensedPanelA);
                    }

                    if (elemA == false) {
                        filteredCombineA = filteredPanelC;
                        condensedCombineA = condensedPanelC;
                        elemA = true;
                    } else {
                        if (chr != 23) {
                            filteredCombineB = filteredPanelC;
                            filteredCombineC = mergeFilesInfo.getCombinedReducedFilteredFile(ttIndex, 0, chr, indexFC);
                            doMergeTwoChunks(parsingArgs, listOfCommands, filteredCombineA, filteredCombineB, filteredCombineC,
                                    Integer.toString(chr), filtered);
                            filteredCombineA = filteredCombineC;

                            // Saving files to remove
                            // listDeleteFiles.add(filteredCombineB);
                            // listDeleteFiles.add(filteredCombineA);

                            indexFC++;
                        } else if (chr == 23) {
                            if (j == minSize) {
                                filteredCombineXA = filteredPanelC;
                            } else {
                                filteredCombineXB = filteredPanelC;
                                filteredCombineXC = mergeFilesInfo.getCombinedReducedFilteredXFile(ttIndex, 0, chr, indexXFC);
                                doMergeTwoChunks(parsingArgs, listOfCommands, filteredCombineXA, filteredCombineXB, filteredCombineXC,
                                        Integer.toString(chr), filtered);
                                filteredCombineXA = filteredCombineXC;

                                // Saving files to remove
                                // listDeleteFiles.add(filteredCombineXB);
                                // listDeleteFiles.add(filteredCombineXA);

                                indexXFC++;
                            }
                        }

                        condensedCombineB = condensedPanelC;
                        condensedCombineC = mergeFilesInfo.getCombinedReducedCondensedFile(ttIndex, 0, chr, indexCC);
                        doMergeTwoChunks(parsingArgs, listOfCommands, condensedCombineA, condensedCombineB, condensedCombineC,
                                Integer.toString(chr), condensed);
                        condensedCombineA = condensedCombineC;
                        indexCC++;

                        /*
                         * File fA = new File(condensedCombineA); fA.delete(); File fB = new File(condensedCombineB);
                         * fB.delete();
                         */
                        // Saving files to remove
                        listDeleteFiles.add(condensedCombineB);
                        listDeleteFiles.add(condensedCombineA);

                    }

                    lim1 = lim1 + chunkSize;
                    lim2 = lim2 + chunkSize;

                } // END for size

            } // END for chromo

            // Remove intermediate files
            /*
             * for(int i=0;i<listDeleteFiles.size(); i++) {
             * 
             * String fileName = listDeleteFiles.get(i);
             * 
             * // Remove file different from last "C" files if((fileName != condensedPanelC) && (fileName !=
             * condensedCombineC)) { //&& (fileName != filteredCombineC) && (fileName != filteredCombineXC)) { File f =
             * new File(fileName); f.delete(); } }
             * 
             */

            // Finally, we create topHits, QQ and Manhattan plots.
            String topHitsCombinedResults = combinedPanelsFilesInfo.getTopHitsFile(ttIndex);

            doGenerateTopHits(parsingArgs, listOfCommands, filteredCombineC, filteredCombineXC, topHitsCombinedResults, PVA_THRESHOLD_STR);

            String combinedQqPlotPdfFile = combinedPanelsFilesInfo.getQqPlotPdfFile(ttIndex);
            String combinedQqPlotTiffFile = combinedPanelsFilesInfo.getQqPlotTiffFile(ttIndex);
            String combinedManhattanPlotPdfFile = combinedPanelsFilesInfo.getManhattanPdfFile(ttIndex);
            String combinedManhattanPlotTiffFile = combinedPanelsFilesInfo.getManhattanTiffFile(ttIndex);
            String combinedCorrectedPvaluesFile = combinedPanelsFilesInfo.getCorrectedPvaluesFile(ttIndex);

            doGenerateQQManhattanPlots(parsingArgs, listOfCommands, condensedCombineC, combinedQqPlotPdfFile, combinedManhattanPlotPdfFile,
                    combinedQqPlotTiffFile, combinedManhattanPlotTiffFile, combinedCorrectedPvaluesFile);
        }
    }

    /**
     * Method that performs the last part of the work flow corresponding to the phenome Analysis, combining the results
     * of each phenoType analysis
     * 
     * @param parsingArgs
     * @param mergeFilesInfo
     * @param resultsFilesInfo
     * @param phenomeAnalysisFilesInfo
     * @param rpanelTypes
     * @param listOfCommands
     */
    private static void makePhenotypeAnalysis(ParseCmdLine parsingArgs, MergeFiles mergeFilesInfo, ResultsFiles resultsFilesInfo,
            PhenomeAnalysisFiles phenomeAnalysisFilesInfo, List<String> rpanelTypes, ArrayList<String> listOfCommands) {

        int endChr = parsingArgs.getEnd();
        String endChrS = Integer.toString(endChr);

        String topHitsFile = null;

        String filteredByAllFile = null;
        String filteredByAllXFile = null;

        String phenomeFileA = null;
        String phenomeFileB = null;
        String phenomeFileC = null;

        int numberOfTestTypes = parsingArgs.getNumberOfTestTypeName();
        int numberOfRpanelsTypes = rpanelTypes.size();

        // int maxPhenoIndex = numberOfTestTypes * numberOfRpanelsTypes - 1;
        int phenoIndex = 0;

        int ttIndex = 0;
        int rpIndex = 0;
        String ttName = parsingArgs.getTestTypeName(ttIndex);
        String rpName = rpanelTypes.get(rpIndex);

        topHitsFile = resultsFilesInfo.getTopHitsFile(ttIndex, rpIndex);
        // filteredByAllFile = mergeFilesInfo.getFinalFilteredByAllFile(ttIndex, rpIndex);
        // phenomeFileA = phenomeAnalysisFilesInfo.getPhenotypeFile(phenoIndex);
        phenomeFileA = phenomeAnalysisFilesInfo.getPhenotypeIntermediateFile(phenoIndex);
        phenoIndex++;

        doInitPhenoMatrix(parsingArgs, listOfCommands, topHitsFile, ttName, rpName, phenomeFileA);

        for (ttIndex = 0; ttIndex < numberOfTestTypes; ttIndex++) {
            int startRp = 0;
            if (ttIndex == 0) {
                startRp = 1;
            }
            ttName = parsingArgs.getTestTypeName(ttIndex);
            for (rpIndex = startRp; rpIndex < numberOfRpanelsTypes; rpIndex++) {
                rpName = rpanelTypes.get(rpIndex);

                topHitsFile = resultsFilesInfo.getTopHitsFile(ttIndex, rpIndex);
                // filteredByAllFile = mergeFilesInfo.getFinalFilteredByAllFile(ttIndex, rpIndex);
                // condensedFile = mergeFilesInfo.getFinalCondensedFile(ttIndex, rpIndex);

                // phenomeFileB = phenomeAnalysisFilesInfo.getPhenotypeFile(phenoIndex);
                phenomeFileB = phenomeAnalysisFilesInfo.getPhenotypeIntermediateFile(phenoIndex);

                doAddToPhenoMatrix(parsingArgs, listOfCommands, phenomeFileA, topHitsFile, ttName, rpName, phenomeFileB);

                phenomeFileA = phenomeFileB;
                phenoIndex++;
            }
        }

        phenoIndex = 0;
        // Lets do the fillinout of the phenomeAnalysis.
        for (ttIndex = 0; ttIndex < numberOfTestTypes; ttIndex++) {
            ttName = parsingArgs.getTestTypeName(ttIndex);

            for (rpIndex = 0; rpIndex < numberOfRpanelsTypes; rpIndex++) {
                rpName = rpanelTypes.get(rpIndex);

                // condensedFile = mergeFilesInfo.getFinalCondensedFile(ttIndex, rpIndex);
                filteredByAllFile = mergeFilesInfo.getFinalFilteredByAllFile(ttIndex, rpIndex);
                if (endChrS.equals("23")) {
                    filteredByAllXFile = mergeFilesInfo.getAdditionalFilteredByAllXFile(ttIndex, rpIndex, 0);
                } else {
                    filteredByAllXFile = mergeFilesInfo.getFinalFilteredByAllFile(ttIndex, rpIndex);
                }

                phenomeFileB = phenomeAnalysisFilesInfo.getPhenotypeFile(phenoIndex);

                doFilloutPhenoMatrix(parsingArgs, listOfCommands, phenomeFileA, filteredByAllFile, filteredByAllXFile, endChrS, ttName,
                        rpName, phenomeFileB);

                // phenomeFileA = phenomeFileB;
                phenoIndex++;
            }
        }

        // Last round to generate final results
        phenoIndex = 0;
        phenomeFileA = phenomeAnalysisFilesInfo.getPhenotypeFile(phenoIndex);

        phenoIndex++;

        // Lets do the finalization of the phenomeAnalysis.
        for (ttIndex = 0; ttIndex < numberOfTestTypes; ttIndex++) {
            int startRp = 0;
            if (ttIndex == 0) {
                startRp = 1;
            }

            ttName = parsingArgs.getTestTypeName(ttIndex);
            for (rpIndex = startRp; rpIndex < numberOfRpanelsTypes; rpIndex++) {
                rpName = rpanelTypes.get(rpIndex);
                // System.out.println("i\n\n\t[makeImputationPerChunk]: ttName " + ttName + " | rpName " + rpName + " |
                // phenoIndex " + phenoIndex);
                // System.out.println("\t[makeImputationPerChunk]: phenomeFileA " + phenomeFileA);
                phenomeFileB = phenomeAnalysisFilesInfo.getPhenotypeFile(phenoIndex);
                // System.out.println("\t[makeImputationPerChunk]: phenomeFileB " + phenomeFileB);

                phenomeFileC = phenomeAnalysisFilesInfo.getPhenotypeFinalFile(phenoIndex);
                // System.out.println("\t[makeImputationPerChunk]: phenomeFileC " + phenomeFileC);

                doFinalizePhenoMatrix(parsingArgs, listOfCommands, phenomeFileA, phenomeFileB, ttName, rpName, phenomeFileC);

                phenomeFileA = phenomeFileC;
                phenoIndex++;
            }
        }

    }

    /**
     * Method the wraps the execution of convertFromBedToBed tasks and store the command in the listOfCommands
     * 
     * @param parsingArgs
     * @param listOfCommands
     * @param bedFile
     * @param bimFile
     * @param famFile
     * @param mixedBedFile
     * @param mixedBimFile
     * @param mixedFamFile
     * @param mixedBedToBedLogFile
     * @param theChromo
     */
    private static void doConvertFromBedToBed(ParseCmdLine parsingArgs, ArrayList<String> listOfCommands, String bedFile, String bimFile,
            String famFile, String mixedBedFile, String mixedBimFile, String mixedFamFile, String mixedBedToBedLogFile, String theChromo) {

        String cmdToStore = null;
        if (parsingArgs.getStageStatus("convertFromBedToBed") == 1) {
            // Task
            cmdToStore = JAVA_HOME + "/java convertFromBedToBed.jar " + bedFile + " " + bimFile + " " + famFile + " " + mixedBedFile + " "
                    + mixedBimFile + " " + mixedFamFile + " " + mixedBedToBedLogFile + " " + theChromo;
            listOfCommands.add(cmdToStore);
            try {
                GuidanceImpl.convertFromBedToBed(bedFile, bimFile, famFile, mixedBedFile, mixedBimFile, mixedFamFile, mixedBedToBedLogFile,
                        theChromo, cmdToStore);
            } catch (GuidanceTaskException gte) {
                System.err.println("[Guidance] Exception trying the execution of convertFromBedToBed task");
                System.err.println(gte.getMessage());
            }
        }
    }

    /**
     * Method the wraps the execution of createRsIdList task and store the command in the listOfCommands
     * 
     * @param parsingArgs
     * @param listOfCommands
     * @param mixedBimOrGenFile
     * @param exclCgatFlag
     * @param mixedPairsFile
     * @param inputFormat
     */
    private static void doCreateRsIdList(ParseCmdLine parsingArgs, ArrayList<String> listOfCommands, String mixedBimOrGenFile,
            String exclCgatFlag, String mixedPairsFile, String inputFormat) {

        String cmdToStore = " ";
        if (parsingArgs.getStageStatus("createRsIdList") == 1) {
            // Task
            cmdToStore = JAVA_HOME + "/java createRsIdList " + mixedBimOrGenFile + " " + exclCgatFlag + " " + mixedPairsFile + " "
                    + inputFormat;
            listOfCommands.add(cmdToStore);
            try {
                GuidanceImpl.createRsIdList(mixedBimOrGenFile, exclCgatFlag, mixedPairsFile, inputFormat, cmdToStore);
            } catch (GuidanceTaskException gte) {
                System.err.println("[Guidance] Exception trying the execution of createRsIdList task");
                System.err.println(gte.getMessage());
            }
        }
    }

    /**
     * Method that wraps the execution of phasing task with bed input formats and store the command in the
     * listOfCommands
     * 
     * @param parsingArgs
     * @param listOfCommands
     * @param theChromo
     * @param bedFile
     * @param bimFile
     * @param famFile
     * @param gmapFile
     * @param excludedSnpsFile
     * @param shapeitHapsFile
     * @param shapeitSampleFile
     * @param shapeitLogFile
     * @param filteredHaplotypesFile
     * @param filteredHaplotypesSampleFile
     * @param filteredHaplotypesLogFile
     * @param filteredHaplotypesVcfFile
     * @param listOfSnpsFile
     * @param exclCgatFlag
     * @param exclSVFlag
     */
    private static void doPhasingBed(ParseCmdLine parsingArgs, ArrayList<String> listOfCommands, String theChromo, String bedFile,
            String bimFile, String famFile, String gmapFile, String excludedSnpsFile, String shapeitHapsFile, String shapeitSampleFile,
            String shapeitLogFile, String filteredHaplotypesFile, String filteredHaplotypesSampleFile, String filteredHaplotypesLogFile,
            String filteredHaplotypesVcfFile, String listOfSnpsFile, String exclCgatFlag, String exclSVFlag) {

        String cmdToStore = " ";
        if (parsingArgs.getStageStatus("phasingBed") == 1) {
            // If we process chromoso X (23) then we change the cmdToStore
            if (theChromo.equals("23")) {
                cmdToStore = SHAPEIT_BINARY + " --input-bed " + bedFile + " " + bimFile + " " + famFile + " --input-map " + gmapFile
                        + " --chrX --output-max " + shapeitHapsFile + " " + shapeitSampleFile
                        + " --thread 47 --effective-size 20000 --output-log " + shapeitLogFile;
            } else {
                cmdToStore = SHAPEIT_BINARY + " --input-bed " + bedFile + " " + bimFile + " " + famFile + " --input-map " + gmapFile
                        + " --output-max " + shapeitHapsFile + " " + shapeitSampleFile + " --thread 47 --effective-size 20000 --output-log "
                        + shapeitLogFile;
            }

            listOfCommands.add(cmdToStore);
            try {
                GuidanceImpl.phasingBed(theChromo, bedFile, bimFile, famFile, gmapFile, shapeitHapsFile, shapeitSampleFile, shapeitLogFile,
                        cmdToStore);
            } catch (GuidanceTaskException gte) {
                System.err.println("[Guidance] Exception trying the execution of phasing task");
                System.err.println(gte.getMessage());
            }
        }

        String cmdToStore2 = " ";
        if (parsingArgs.getStageStatus("createListOfExcludedSnps") == 1) {
            cmdToStore2 = JAVA_HOME + "/java createListOfExcludedSnps.jar " + shapeitHapsFile + " " + excludedSnpsFile + " " + exclCgatFlag
                    + " " + exclSVFlag;

            listOfCommands.add(cmdToStore2);
            try {
                GuidanceImpl.createListOfExcludedSnps(shapeitHapsFile, excludedSnpsFile, exclCgatFlag, exclSVFlag, cmdToStore2);
            } catch (GuidanceTaskException gte) {
                System.err.println("[Guidance] Exception trying the execution of createListOfExcludedSnps task");
                System.err.println(gte.getMessage());
            }
        }

        String cmdToStore3 = " ";
        if (parsingArgs.getStageStatus("filterHaplotypes") == 1) {
            cmdToStore3 = JAVA_HOME + "/java filterHaplotypes.jar " + shapeitHapsFile + " " + shapeitSampleFile + " " + excludedSnpsFile
                    + " " + filteredHaplotypesFile + " " + filteredHaplotypesSampleFile + " " + filteredHaplotypesLogFile + " "
                    + filteredHaplotypesVcfFile + " " + listOfSnpsFile;

            listOfCommands.add(cmdToStore3);
            try {
                GuidanceImpl.filterHaplotypes(shapeitHapsFile, shapeitSampleFile, excludedSnpsFile, filteredHaplotypesFile,
                        filteredHaplotypesSampleFile, filteredHaplotypesLogFile, filteredHaplotypesVcfFile, listOfSnpsFile, cmdToStore3);
            } catch (GuidanceTaskException gte) {
                System.err.println("[Guidance] Exception trying the execution of createListOfExcludedSnps task");
                System.err.println(gte.getMessage());
            }
        }
    }

    /**
     * Method that wraps the execution of phasing task with gen input formats and store the command in the
     * listOfCommands
     * 
     * @param parsingArgs
     * @param listOfCommands
     * @param theChromo
     * @param genFile
     * @param sampleFile
     * @param gmapFile
     * @param excludedSnpsFile
     * @param shapeitHapsFile
     * @param shapeitSampleFile
     * @param shapeitLogFile
     * @param filteredHaplotypesFile
     * @param filteredHaplotypesSampleFile
     * @param filteredHaplotypesLogFile
     * @param filteredHaplotypesVcfFile
     * @param listOfSnpsFile
     * @param exclCgatFlag
     * @param exclSVFlag
     */
    private static void doPhasing(ParseCmdLine parsingArgs, ArrayList<String> listOfCommands, String theChromo, String genFile,
            String sampleFile, String gmapFile, String excludedSnpsFile, String shapeitHapsFile, String shapeitSampleFile,
            String shapeitLogFile, String filteredHaplotypesFile, String filteredHaplotypesSampleFile, String filteredHaplotypesLogFile,
            String filteredHaplotypesVcfFile, String listOfSnpsFile, String exclCgatFlag, String exclSVFlag) {

        String cmdToStore = null;
        if (parsingArgs.getStageStatus("pashing") == 1) {
            if (theChromo.equals("23")) {
                // If we process chromoso X (23) then we change the cmdToStore
                cmdToStore = SHAPEIT_BINARY + " --input-gen " + genFile + " " + sampleFile + " --input-map " + gmapFile
                        + " --chrX --output-max " + shapeitHapsFile + " " + shapeitSampleFile
                        + " --thread 15 --effective-size 20000 --output-log " + shapeitLogFile;
            } else {
                cmdToStore = SHAPEIT_BINARY + " --input-gen " + genFile + " " + sampleFile + " --input-map " + gmapFile + " --output-max "
                        + shapeitHapsFile + " " + shapeitSampleFile + " --thread 15 --effective-size 20000 --output-log " + shapeitLogFile;
            }
            listOfCommands.add(cmdToStore);
            try {
                GuidanceImpl.phasing(theChromo, genFile, sampleFile, gmapFile, shapeitHapsFile, shapeitSampleFile, shapeitLogFile,
                        cmdToStore);
            } catch (GuidanceTaskException gte) {
                System.err.println("[Guidance] Exception trying the execution of phasing task");
                System.err.println(gte.getMessage());
            }
        }

        String cmdToStore2 = " ";
        if (parsingArgs.getStageStatus("createListOfExcludedSnps") == 1) {
            cmdToStore2 = JAVA_HOME + "/java createListOfExcludedSnps.jar " + shapeitHapsFile + " " + excludedSnpsFile + " " + exclCgatFlag
                    + " " + exclSVFlag;

            listOfCommands.add(cmdToStore2);
            try {
                GuidanceImpl.createListOfExcludedSnps(shapeitHapsFile, excludedSnpsFile, exclCgatFlag, exclSVFlag, cmdToStore2);
            } catch (GuidanceTaskException gte) {
                System.err.println("[Guidance] Exception trying the execution of createListOfExcludedSnps task");
                System.err.println(gte.getMessage());
            }
        }

        String cmdToStore3 = " ";
        if (parsingArgs.getStageStatus("filterHaplotypes") == 1) {
            cmdToStore3 = JAVA_HOME + "/java filterHaplotypes.jar " + shapeitHapsFile + " " + shapeitSampleFile + " " + excludedSnpsFile
                    + " " + filteredHaplotypesFile + " " + filteredHaplotypesSampleFile + " " + filteredHaplotypesLogFile + " "
                    + filteredHaplotypesVcfFile + " " + listOfSnpsFile;

            listOfCommands.add(cmdToStore3);
            try {
                GuidanceImpl.filterHaplotypes(shapeitHapsFile, shapeitSampleFile, excludedSnpsFile, filteredHaplotypesFile,
                        filteredHaplotypesSampleFile, filteredHaplotypesLogFile, filteredHaplotypesVcfFile, listOfSnpsFile, cmdToStore3);
            } catch (GuidanceTaskException gte) {
                System.err.println("[Guidance] Exception trying the execution of createListOfExcludedSnps task");
                System.err.println(gte.getMessage());
            }
        }
    }

    /**
     * Method that wraps the execution of impute task and store the command in the listOfCommands
     * 
     * @param parsingArgs
     * @param listOfCommands
     * @param chrS
     * @param gmapFile
     * @param knownHapFile
     * @param legendFile
     * @param shapeitHapsFile
     * @param shapeitSampleFile
     * @param lim1S
     * @param lim2S
     * @param pairsFile
     * @param imputeFile
     * @param imputeFileInfo
     * @param imputeFileSummary
     * @param imputeFileWarnings
     */
    private static void doImputationWithImpute(ParseCmdLine parsingArgs, ArrayList<String> listOfCommands, String chrS, String gmapFile,
            String knownHapFile, String legendFile, String shapeitHapsFile, String shapeitSampleFile, String lim1S, String lim2S,
            String pairsFile, String imputeFile, String imputeFileInfo, String imputeFileSummary, String imputeFileWarnings) {

        String cmdToStore = null;
        if (parsingArgs.getStageStatus("imputeWithImpute") == 1) {
            // Submitting the impute task per chunk

            if (chrS.equals("23")) {
                cmdToStore = IMPUTE2_BINARY + " -use_prephased_g -m " + gmapFile + " -h " + knownHapFile + " -l " + legendFile
                        + " -known_haps_g " + shapeitHapsFile + " -sample_g " + shapeitSampleFile + " -int " + lim1S + " " + lim2S
                        + " -chrX -exclude_snps_g " + pairsFile + " -impute_excluded -Ne 20000 -o " + imputeFile + " -i " + imputeFileInfo
                        + " -r " + imputeFileSummary + " -w " + imputeFileWarnings + " -no_sample_qc_info -o_gz ";
            } else {
                cmdToStore = IMPUTE2_BINARY + " -use_prephased_g -m " + gmapFile + " -h " + knownHapFile + " -l " + legendFile
                        + " -known_haps_g " + shapeitHapsFile + " -int " + lim1S + " " + lim2S + " -exclude_snps_g " + pairsFile
                        + " -impute_excluded -Ne 20000 -o " + imputeFile + " -i " + imputeFileInfo + " -r " + imputeFileSummary + " -w "
                        + imputeFileWarnings + " -no_sample_qc_info -o_gz";
            }
            listOfCommands.add(cmdToStore);
            try {
                GuidanceImpl.imputeWithImpute(gmapFile, knownHapFile, legendFile, shapeitHapsFile, shapeitSampleFile, lim1S, lim2S,
                        pairsFile, imputeFile, imputeFileInfo, imputeFileSummary, imputeFileWarnings, chrS, cmdToStore);
            } catch (GuidanceTaskException gte) {
                System.err.println("[Guidance] Exception trying the execution of impute task");
                System.err.println(gte.getMessage());
            }
        }
    }

    /**
     * Method that wraps the execution of Imputation task with Minimac2 and store the command in the listOfCommands
     * 
     * @param parsingArgs
     * @param listOfCommands
     * @param knownHapFile
     * @param filteredHapsFile
     * @param filteredSampleFile
     * @param filteredListOfSnpsFile
     * @param imputedMMFileName
     * @param imputedMMInfoFile
     * @param imputedMMErateFile
     * @param imputedMMRecFile
     * @param imputedMMDoseFile
     * @param imputedMMLogFile
     * @param chrS
     * @param lim1S
     * @param lim2S
     */
    private static void doImputationWithMinimac(ParseCmdLine parsingArgs, ArrayList<String> listOfCommands, String knownHapFile,
            String filteredHapsFile, String filteredSampleFile, String filteredListOfSnpsFile, String imputedMMFileName,
            String imputedMMInfoFile, String imputedMMErateFile, String imputedMMRecFile, String imputedMMDoseFile, String imputedMMLogFile,
            String chrS, String lim1S, String lim2S) {

        String cmdToStore = null;
        if (parsingArgs.getStageStatus("imputeWithMinimac") == 1) {
            // Submitting the impute task per chunk
            // We don't distinguish chrS 23 since the cmdToStore is the same
            cmdToStore = JAVA_HOME + "/java imputationWithMinimac --vcfReference --refHaps " + knownHapFile + " --shape_haps "
                    + filteredHapsFile + " --sample " + filteredSampleFile + " --snps " + filteredListOfSnpsFile + " --vcfstart " + lim1S
                    + " --vcfend " + lim2S + " --chr " + chrS + " --vcfwindow --rounds 5 --states 200 --outInfo " + imputedMMInfoFile
                    + " --outErate " + imputedMMErateFile + " --outRec " + imputedMMRecFile + " --outDose " + imputedMMDoseFile
                    + " --outLog " + imputedMMLogFile;

            listOfCommands.add(cmdToStore);

            try {
                GuidanceImpl.imputeWithMinimac(knownHapFile, filteredHapsFile, filteredSampleFile, filteredListOfSnpsFile,
                        imputedMMFileName, imputedMMInfoFile, imputedMMErateFile, imputedMMRecFile, imputedMMDoseFile, imputedMMLogFile,
                        chrS, lim1S, lim2S, cmdToStore);
            } catch (GuidanceTaskException gte) {
                System.err.println("[Guidance] Exception trying the execution of imputationWithMinimac task");
                System.err.println(gte.getMessage());
            }
        }
    }

    /**
     * Method that wraps the execution of filterByInfo task and store the command in the listOfCommands
     * 
     * @param parsingArgs
     * @param listOfCommands
     * @param imputeFileInfo
     * @param filteredRsIdFile
     * @param infoThresholdS
     */
    private static void doFilterByInfo(ParseCmdLine parsingArgs, ArrayList<String> listOfCommands, String imputeFileInfo,
            String filteredRsIdFile, String infoThresholdS) {

        String cmdToStore = null;
        if (parsingArgs.getStageStatus("filterByInfo") == 1) {
            // We create the list of rsId that are greater than or equal to the infoThreshold value
            cmdToStore = JAVA_HOME + "/java filterByInfo " + imputeFileInfo + " " + filteredRsIdFile + " " + infoThresholdS;
            listOfCommands.add(cmdToStore);
            try {
                GuidanceImpl.filterByInfo(imputeFileInfo, filteredRsIdFile, infoThresholdS, cmdToStore);
            } catch (GuidanceTaskException gte) {
                System.err.println("[Guidance] Exception trying the execution of filterByInfo tasks for controls");
                System.err.println(gte.getMessage());
            }
        }
    }

    /**
     * Method that wraps the execution of qctoolS task and store the command in the listOfCommands
     * 
     * @param parsingArgs
     * @param listOfCommands
     * @param imputeFile
     * @param filteredRsIdFile
     * @param filteredFile
     * @param filteredLogFile
     */
    private static void doQctoolS(ParseCmdLine parsingArgs, ArrayList<String> listOfCommands, String imputeFile, String filteredRsIdFile,
            String filteredFile, String filteredLogFile) {

        double mafThreshold = parsingArgs.getMafThreshold();
        String mafThresholdS = Double.toString(mafThreshold);

        String cmdToStore = null;
        if (parsingArgs.getStageStatus("qctoolS") == 1) {
            cmdToStore = QCTOOL_BINARY + " -g " + imputeFile + " -og " + filteredFile + " -incl-rsids " + filteredRsIdFile
                    + " -omit-chromosome -force -log " + filteredLogFile + " -maf " + mafThresholdS + " 1";
            listOfCommands.add(cmdToStore);
            try {
                GuidanceImpl.qctoolS(imputeFile, filteredRsIdFile, mafThresholdS, filteredFile, filteredLogFile, cmdToStore);
            } catch (GuidanceTaskException gte) {
                System.err.println("[Guidance] Exception trying the execution of qctoolS tasks for controls");
                System.err.println(gte.getMessage());
            }
        }
    }

    /**
     * Method that wraps the execution of snptest task and store the command in the listOfCommands
     * 
     * @param parsingArgs
     * @param listOfCommands
     * @param chrS
     * @param mergedGenFile
     * @param mergedSampleFile
     * @param snptestOutFile
     * @param snptestLogFile
     * @param responseVar
     * @param covariables
     */
    private static void doSnptest(ParseCmdLine parsingArgs, ArrayList<String> listOfCommands, String chrS, String mergedGenFile,
            String mergedSampleFile, String snptestOutFile, String snptestLogFile, String responseVar, String covariables) {

        String cmdToStore = null;
        String newStr = covariables.replace(',', ' ');

        if (parsingArgs.getStageStatus("snptest") == 1) {
            if (covariables.equals("none")) {
                cmdToStore = SNPTEST_BINARY + " -data " + mergedGenFile + " " + mergedSampleFile + " -o " + snptestOutFile + " -pheno "
                        + responseVar + " -hwe -log " + snptestLogFile;
            } else {
                cmdToStore = SNPTEST_BINARY + " -data " + mergedGenFile + " " + mergedSampleFile + " -o " + snptestOutFile + " -pheno "
                        + responseVar + " -cov_names " + newStr + " -hwe -log " + snptestLogFile;
            }

            // Different parameters for chromo 23 (X) and the rest.
            if (chrS.equals("23")) {
                cmdToStore = cmdToStore + " -method newml -assume_chromosome X -stratify_on sex -frequentist 1";
            } else {
                cmdToStore = cmdToStore + " -method em -frequentist 1 2 3 4 5";
            }

            listOfCommands.add(cmdToStore);

            // Submitting the snptest task per this chunk
            try {
                GuidanceImpl.snptest(mergedGenFile, mergedSampleFile, snptestOutFile, snptestLogFile, responseVar, covariables, chrS,
                        cmdToStore);
            } catch (GuidanceTaskException gte) {
                System.err.println("[Guidance] Exception trying the execution of snptest task");
                System.err.println(gte.getMessage());
            }
        }
    }

    /**
     * Method that wraps the execution of collectSummary task and store the command in the listOfCommands
     * 
     * @param parsingArgs
     * @param listOfCommands
     * @param chrS
     * @param imputeFileInfo
     * @param snptestOutFile
     * @param summaryFile
     * @param mafThresholdS
     * @param infoThresholdS
     * @param hweCohortThresholdS
     * @param hweCasesThresholdS
     * @param hweControlsThresholdS
     */
    private static void doCollectSummary(ParseCmdLine parsingArgs, ArrayList<String> listOfCommands, String chrS, String imputeFileInfo,
            String snptestOutFile, String summaryFile, String mafThresholdS, String infoThresholdS, String hweCohortThresholdS,
            String hweCasesThresholdS, String hweControlsThresholdS) {
        String cmdToStore = null;

        if (parsingArgs.getStageStatus("collectSummary") == 1) {
            // Submitting the collect_summary task per this chunk
            cmdToStore = JAVA_HOME + "/java collectSummary " + chrS + " " + imputeFileInfo + " " + snptestOutFile + " " + summaryFile + " "
                    + mafThresholdS + " " + infoThresholdS + " " + hweCohortThresholdS + " " + hweCasesThresholdS + " "
                    + hweControlsThresholdS;

            listOfCommands.add(cmdToStore);

            try {
                GuidanceImpl.collectSummary(chrS, imputeFileInfo, snptestOutFile, summaryFile, mafThresholdS, infoThresholdS,
                        hweCohortThresholdS, hweCasesThresholdS, hweControlsThresholdS, cmdToStore);
            } catch (GuidanceTaskException gte) {
                System.err.println("[Guidance] Exception trying the execution of collectSummary task");
                System.err.println(gte.getMessage());
            }
        }
    }

    /**
     * Method that wraps the jointCondensedFiles task and store the command in the listOfCommands
     * 
     * @param parsingArgs
     * @param listOfCommands
     * @param condensedA
     * @param condensedB
     * @param condensedC
     */
    private static void doJointCondenseFiles(ParseCmdLine parsingArgs, ArrayList<String> listOfCommands, String condensedA,
            String condensedB, String condensedC) {
        String cmdToStore = null;
        if (parsingArgs.getStageStatus("jointCondensedFiles") == 1) {
            cmdToStore = JAVA_HOME + "/java jointCondensedFiles " + condensedA + " " + condensedB + " " + condensedC;
            listOfCommands.add(cmdToStore);

            try {
                GuidanceImpl.jointCondensedFiles(condensedA, condensedB, condensedC, cmdToStore);
            } catch (GuidanceTaskException gte) {
                System.err.println("[Guidance] Exception trying the execution of jointCondensedFiles task");
                System.err.println(gte.getMessage());
            }
        }
    }

    /**
     * Method that wraps the doJointFilteredByAllFiles task and store the command in the listOfCommands
     * 
     * @param parsingArgs
     * @param listOfCommands
     * @param filteredByAllA
     * @param filteredByAllB
     * @param filteredByAllC
     * @param rpanelName
     * @param rpanelFlag
     */
    private static void doJointFilteredByAllFiles(ParseCmdLine parsingArgs, ArrayList<String> listOfCommands, String filteredByAllA,
            String filteredByAllB, String filteredByAllC, String rpanelName, String rpanelFlag) {
        String cmdToStore = null;
        if (parsingArgs.getStageStatus("jointFilteredByAllFiles") == 1) {
            cmdToStore = JAVA_HOME + "/java jointFilteredByAllFiles " + filteredByAllA + " " + filteredByAllB + " " + filteredByAllC + " "
                    + rpanelName + " " + rpanelFlag;
            listOfCommands.add(cmdToStore);
            try {
                GuidanceImpl.jointFilteredByAllFiles(filteredByAllA, filteredByAllB, filteredByAllC, rpanelName, rpanelFlag, cmdToStore);
            } catch (GuidanceTaskException gte) {
                System.err.println("[Guidance] Exception trying the execution of getBestSnps task");
                System.err.println("The error message here is " + gte.getMessage());
            }
        }
    }

    /**
     * Method that wraps the generateTopHits task and store the command in the listOfCommands
     * 
     * @param parsingArgs
     * @param listOfCommands
     * @param filteredFile
     * @param filteredXFile
     * @param topHitsResults
     * @param pvaThrS
     */
    private static void doGenerateTopHits(ParseCmdLine parsingArgs, ArrayList<String> listOfCommands, String filteredFile,
            String filteredXFile, String topHitsResults, String pvaThrS) {

        String cmdToStore = null;
        if (parsingArgs.getStageStatus("generateTopHits") == 1) {
            cmdToStore = JAVA_HOME + "/java generateTopHits " + filteredFile + " " + filteredXFile + " " + topHitsResults + " " + pvaThrS;
            listOfCommands.add(cmdToStore);
            try {
                GuidanceImpl.generateTopHitsAll(filteredFile, filteredXFile, topHitsResults, pvaThrS, cmdToStore);
            } catch (GuidanceTaskException gte) {
                System.err.println("[Guidance] Exception trying the execution of generateTopHits task");
                System.err.println(gte.getMessage());
            }
        }
    }

    /**
     * Method that wraps the generateQQManhattanPlots task and store the command in the listOfCommands
     * 
     * @param parsingArgs
     * @param listOfCommands
     * @param condensedFile
     * @param qqPlotFile
     * @param manhattanPlotFile
     * @param qqPlotTiffFile
     * @param manhattanPlotTiffFile
     * @param correctedPvaluesFile
     */
    private static void doGenerateQQManhattanPlots(ParseCmdLine parsingArgs, ArrayList<String> listOfCommands, String condensedFile,
            String qqPlotFile, String manhattanPlotFile, String qqPlotTiffFile, String manhattanPlotTiffFile, String correctedPvaluesFile) {
        String cmdToStore = null;
        if (parsingArgs.getStageStatus("generateQQManhattanPlots") == 1) {
            cmdToStore = R_SCRIPT_BIN_DIR + "/Rscript " + R_SCRIPT_DIR + "/qqplot_manhattan.R " + condensedFile + " " + qqPlotFile + " "
                    + manhattanPlotFile + " " + qqPlotTiffFile + " " + manhattanPlotTiffFile + " " + correctedPvaluesFile;
            listOfCommands.add(cmdToStore);

            try {
                GuidanceImpl.generateQQManhattanPlots(condensedFile, qqPlotFile, manhattanPlotFile, qqPlotTiffFile, manhattanPlotTiffFile,
                        correctedPvaluesFile, cmdToStore);
            } catch (GuidanceTaskException gte) {
                System.err.println("[Guidance] Exception trying the execution of generateQQManhattanPlots task");
                System.err.println(gte.getMessage());
            }
        }
    }

    /**
     * Method that wraps the combinePanelsComplex task and store the command in the listOfCommands
     * 
     * @param parsingArgs
     * @param listOfCommands
     * @param resultsPanelA
     * @param resultsPanelB
     * @param lastResultFile
     * @param lim1
     * @param lim2
     */
    private static void doCombinePanelsComplex(ParseCmdLine parsingArgs, ArrayList<String> listOfCommands, String resultsPanelA,
            String resultsPanelB, String lastResultFile, int lim1, int lim2) {

        String cmdToStore = null;

        if (parsingArgs.getStageStatus("combinePanelsComplex") == 1) {
            cmdToStore = JAVA_HOME + "/java combinePanelsComplex " + resultsPanelA + " " + resultsPanelB + " " + lastResultFile + " " + lim1
                    + " " + lim2;
            listOfCommands.add(cmdToStore);

            try {
                GuidanceImpl.combinePanelsComplex(resultsPanelA, resultsPanelB, lastResultFile, lim1, lim2, cmdToStore);
            } catch (GuidanceTaskException gte) {
                System.err.println("[Guidance] Exception trying the execution of combinePanelsComplex task");
                System.err.println(gte.getMessage());
            }
        }
    }

    /**
     * Method that wraps the mergeTwoChunks task and store the command in the listOfCommands
     * 
     * @param parsingArgs
     * @param listOfCommands
     * @param reduceA
     * @param reduceB
     * @param reduceC
     * @param theChromo
     * @param type
     */
    private static void doMergeTwoChunks(ParseCmdLine parsingArgs, ArrayList<String> listOfCommands, String reduceA, String reduceB,
            String reduceC, String theChromo, String type) {

        String cmdToStore = null;
        if (parsingArgs.getStageStatus("mergeTwoChunks") == 1) {
            // Task
            cmdToStore = JAVA_HOME + "/java mergeTwoChunks " + reduceA + " " + reduceB + " " + reduceC + " " + theChromo;
            listOfCommands.add(cmdToStore);
            try {
                GuidanceImpl.mergeTwoChunks(reduceA, reduceB, reduceC, theChromo, type, cmdToStore);
            } catch (GuidanceTaskException gte) {
                System.err.println("[Guidance] Exception trying the execution of mergeTwoChunks task");
                System.err.println(gte.getMessage());
            }
        }
    }

    /**
     * Method that wraps the filterByAll task and store the command in the listOfCommands
     * 
     * @param parsingArgs
     * @param listOfCommands
     * @param inputFile
     * @param outputFile
     * @param outputCondensedFile
     */
    private static void doFilterByAll(ParseCmdLine parsingArgs, ArrayList<String> listOfCommands, String inputFile, String outputFile,
            String outputCondensedFile) {

        String cmdToStore = null;

        double mafThreshold = parsingArgs.getMafThreshold();
        double infoThreshold = parsingArgs.getInfoThreshold();
        double hweCohortThreshold = parsingArgs.getHweCohortThreshold();
        double hweCasesThreshold = parsingArgs.getHweCasesThreshold();
        double hweControlsThreshold = parsingArgs.getHweControlsThreshold();

        String mafThresholdS = Double.toString(mafThreshold);
        String infoThresholdS = Double.toString(infoThreshold);
        String hweCohortThresholdS = Double.toString(hweCohortThreshold);
        String hweCasesThresholdS = Double.toString(hweCasesThreshold);
        String hweControlsThresholdS = Double.toString(hweControlsThreshold);

        // Task
        if (parsingArgs.getStageStatus("filterByAll") == 1) {
            cmdToStore = JAVA_HOME + "/java filterByAll " + inputFile + " " + outputFile + " " + outputCondensedFile + " " + mafThresholdS
                    + " " + infoThresholdS + " " + hweCohortThresholdS + " " + hweCasesThresholdS + " " + hweControlsThresholdS;

            listOfCommands.add(cmdToStore);
            try {
                GuidanceImpl.filterByAll(inputFile, outputFile, outputCondensedFile, mafThresholdS, infoThresholdS, hweCohortThresholdS,
                        hweCasesThresholdS, hweControlsThresholdS, cmdToStore);
            } catch (GuidanceTaskException gte) {
                System.err.println("[Guidance] Exception trying the execution of filterByAll task");
                System.err.println(gte.getMessage());
            }
        }
    }

    /**
     * Method that wraps the initPhenoMatrix task and store the command in the listOfCommands
     * 
     * @param parsingArgs
     * @param listOfCommands
     * @param topHitsFile
     * @param ttName
     * @param rpName
     * @param phenomeFile
     */
    private static void doInitPhenoMatrix(ParseCmdLine parsingArgs, ArrayList<String> listOfCommands, String topHitsFile, String ttName,
            String rpName, String phenomeFile) {
        String cmdToStore = null;

        if (parsingArgs.getStageStatus("initPhenoMatrix") == 1) {
            cmdToStore = JAVA_HOME + "/java initPhenoMatrix " + topHitsFile + " " + ttName + " " + rpName + " " + phenomeFile;
            listOfCommands.add(cmdToStore);

            try {
                GuidanceImpl.initPhenoMatrix(topHitsFile, ttName, rpName, phenomeFile, cmdToStore);
            } catch (GuidanceTaskException gte) {
                System.err.println("[Guidance] Exception trying the execution of initPhenoMatrix task");
                System.err.println(gte.getMessage());
            }
        }
    }

    /**
     * Method that wraps the addToPhenoMatrix task and store the command in the listOfCommands
     * 
     * @param parsingArgs
     * @param listOfCommands
     * @param phenomeFileA
     * @param topHitsFile
     * @param ttName
     * @param rpName
     * @param phenomeFileB
     */
    private static void doAddToPhenoMatrix(ParseCmdLine parsingArgs, ArrayList<String> listOfCommands, String phenomeFileA,
            String topHitsFile, String ttName, String rpName, String phenomeFileB) {
        String cmdToStore = null;

        if (parsingArgs.getStageStatus("addToPhenoMatrix") == 1) {
            cmdToStore = JAVA_HOME + "/java addToPhenoMatrix " + phenomeFileA + " " + topHitsFile + " " + ttName + " " + rpName + " "
                    + phenomeFileB;
            listOfCommands.add(cmdToStore);

            try {
                GuidanceImpl.addToPhenoMatrix(phenomeFileA, topHitsFile, ttName, rpName, phenomeFileB, cmdToStore);
            } catch (GuidanceTaskException gte) {
                System.err.println("[Guidance] Exception trying the execution of addToPhenoMatrix task");
                System.err.println(gte.getMessage());
            }
        }
    }

    /**
     * Method that wraps the filloutPhenoMatrix task and store the command in the listOfCommands
     * 
     * @param parsingArgs
     * @param listOfCommands
     * @param phenomeFileA
     * @param filteredByAllFile
     * @param filteredByAllXFile
     * @param endChrS
     * @param ttName
     * @param rpName
     * @param phenomeFileB
     */
    private static void doFilloutPhenoMatrix(ParseCmdLine parsingArgs, ArrayList<String> listOfCommands, String phenomeFileA,
            String filteredByAllFile, String filteredByAllXFile, String endChrS, String ttName, String rpName, String phenomeFileB) {
        String cmdToStore = null;

        if (parsingArgs.getStageStatus("filloutPhenoMatrix") == 1) {
            cmdToStore = JAVA_HOME + "/java filloutPhenoMatrix " + phenomeFileA + " " + filteredByAllFile + " " + filteredByAllXFile + " "
                    + endChrS + " " + ttName + " " + rpName + " " + phenomeFileB;
            listOfCommands.add(cmdToStore);

            try {
                GuidanceImpl.filloutPhenoMatrix(phenomeFileA, filteredByAllFile, filteredByAllXFile, endChrS, ttName, rpName, phenomeFileB,
                        cmdToStore);
            } catch (GuidanceTaskException gte) {
                System.err.println("[Guidance] Exception trying the execution of filloutPhenoMatrix task");
                System.err.println(gte.getMessage());
            }
        }
    }

    /**
     * Method that wraps the finalizePhenoMatrix task and store the command in the listOfCommands
     * 
     * @param parsingArgs
     * @param listOfCommands
     * @param phenomeFileA
     * @param phenomeFileB
     * @param ttName
     * @param rpName
     * @param phenomeFileC
     */
    private static void doFinalizePhenoMatrix(ParseCmdLine parsingArgs, ArrayList<String> listOfCommands, String phenomeFileA,
            String phenomeFileB, String ttName, String rpName, String phenomeFileC) {
        String cmdToStore = null;

        if (parsingArgs.getStageStatus("finalizePhenoMatrix") == 1) {
            cmdToStore = JAVA_HOME + "/java finalizePhenoMatrix " + phenomeFileA + " " + phenomeFileB + " " + ttName + " " + rpName + " "
                    + phenomeFileC;
            listOfCommands.add(cmdToStore);

            try {
                GuidanceImpl.finalizePhenoMatrix(phenomeFileA, phenomeFileB, ttName, rpName, phenomeFileC, cmdToStore);
            } catch (GuidanceTaskException gte) {
                System.err.println("[Guidance] Exception trying the execution of finalizePhenoMatrix task");
                System.err.println(gte.getMessage());
            }
        }
    }

    /**
     * Method to set the final status of each file that has been generated in the execution So far, the three main
     * status are: uncompressed, compressed, and deleted. The default status is: uncompressed. That is the initial
     * status assigned to each file
     * 
     * @param parsingArgs
     * @param commonFilesInfo
     */
    private static void setFinalStatusForCommonFiles(ParseCmdLine parsingArgs, CommonFiles commonFilesInfo) {
        String compressFiles = parsingArgs.getCompressFiles();

        // This status finalStatus is used only for files that can be compressed, uncompressed or deleted.
        // There are sevaral files that will be kept as they have been generated (uncompressed)
        String finalStatus = null;
        // if( removeTempFiles.equals("YES") ) {
        // finalStatus = "deleted";
        // } else
        if (compressFiles.equals("NO")) {
            finalStatus = "uncompressed";
        } else {
            finalStatus = "compressed";
        }

        int startChr = parsingArgs.getStart();
        int endChr = parsingArgs.getEnd();
        for (int i = startChr; i <= endChr; i++) {
            commonFilesInfo.setPairsFileFinalStatus(i, finalStatus);
            commonFilesInfo.setShapeitHapsFileFinalStatus(i, finalStatus);
            commonFilesInfo.setShapeitSampleFileFinalStatus(i, finalStatus);
        }
    }

    /**
     * Method to set the final status of each file that has been generated in the execution So far, the three main
     * status are: uncompressed, compressed, and deleted. The default status is: uncompressed. That is the initial
     * status assigned to each file
     * 
     * @param parsingArgs
     * @param imputationFilesInfo
     * @param generalChromoInfo
     * @param refPanels
     */
    private static void setFinalStatusForImputationFiles(ParseCmdLine parsingArgs, ImputationFiles imputationFilesInfo,
            ChromoInfo generalChromoInfo, List<String> refPanels) {

        int startChr = parsingArgs.getStart();
        int endChr = parsingArgs.getEnd();
        int chunkSize = parsingArgs.getChunkSize();
        String imputationTool = parsingArgs.getImputationTool();
        String compressFiles = parsingArgs.getCompressFiles();

        // This status finalStatus is used only for files that can be compressed, uncompressed or deleted.
        // There are sevaral files that will be kept as they have been generated (uncompressed)
        String finalStatus = null;
        // if( removeTempFiles.equals("YES") ) {
        // finalStatus = "deleted";
        // } else
        if (compressFiles.equals("NO")) {
            finalStatus = "uncompressed";
        } else {
            finalStatus = "compressed";
        }

        if (imputationTool.equals("impute")) {
            for (int j = 0; j < refPanels.size(); j++) {
                // String rPanel = refPanels.get(j);
                for (int i = startChr; i <= endChr; i++) {
                    int chromo = i;
                    int lim1 = 1;
                    int lim2 = lim1 + chunkSize - 1;
                    int numberOfChunks = generalChromoInfo.getMaxSize(chromo) / chunkSize;
                    int module = generalChromoInfo.getMaxSize(chromo) % chunkSize;
                    if (module != 0)
                        numberOfChunks++;

                    for (int k = 0; k < numberOfChunks; k++) {
                        imputationFilesInfo.setImputedFileFinalStatus(j, chromo, lim1, lim2, chunkSize, finalStatus);
                        imputationFilesInfo.setFilteredFileFinalStatus(j, chromo, lim1, lim2, chunkSize, finalStatus);
                        imputationFilesInfo.setImputedInfoFileFinalStatus(j, chromo, lim1, lim2, chunkSize, finalStatus);

                        lim1 = lim1 + chunkSize;
                        lim2 = lim2 + chunkSize;
                    }
                }
            }
        } else if (imputationTool.equals("minimac")) {
            for (int j = 0; j < refPanels.size(); j++) {
                // String rPanel = refPanels.get(j);
                for (int i = startChr; i <= endChr; i++) {
                    int chromo = i;
                    int lim1 = 1;
                    int lim2 = lim1 + chunkSize - 1;
                    int numberOfChunks = generalChromoInfo.getMaxSize(chromo) / chunkSize;
                    int module = generalChromoInfo.getMaxSize(chromo) % chunkSize;
                    if (module != 0)
                        numberOfChunks++;

                    for (int k = 0; k < numberOfChunks; k++) {
                        imputationFilesInfo.setImputedMMInfoFileFinalStatus(j, chromo, lim1, lim2, chunkSize, finalStatus);
                        lim1 = lim1 + chunkSize;
                        lim2 = lim2 + chunkSize;
                    }
                }
            }
        }

    }

    /**
     * Method to set the final status of each file that has been generated in the execution So far, the three main
     * status are: uncompressed, compressed, and deleted. The default status is: uncompressed. That is the initial
     * status assigned to each file
     * 
     * @param parsingArgs
     * @param assocFilesInfo
     * @param generalChromoInfo
     * @param refPanels
     */
    private static void setFinalStatusForAssocFiles(ParseCmdLine parsingArgs, AssocFiles assocFilesInfo, ChromoInfo generalChromoInfo,
            List<String> refPanels) {

        int startChr = parsingArgs.getStart();
        int endChr = parsingArgs.getEnd();
        int chunkSize = parsingArgs.getChunkSize();

        String compressFiles = parsingArgs.getCompressFiles();

        // This status finalStatus is used only for files that can be compressed, uncompressed or deleted.
        // There are sevaral files that will be kept as they have been generated (uncompressed)
        String finalStatus = null;
        // if( removeTempFiles.equals("YES") ) {
        // finalStatus = "deleted";
        // } else
        if (compressFiles.equals("NO")) {
            finalStatus = "uncompressed";
        } else {
            finalStatus = "compressed";
        }

        // Now we continue with the association
        int numberOfTestTypes = parsingArgs.getNumberOfTestTypeName();
        for (int tt = 0; tt < numberOfTestTypes; tt++) {
            for (int j = 0; j < refPanels.size(); j++) {
                // String rPanel = refPanels.get(j);
                for (int i = startChr; i <= endChr; i++) {
                    int chromo = i;

                    int maxSize = generalChromoInfo.getMaxSize(chromo);
                    int total_chunks = maxSize / chunkSize;
                    int lim1 = 1;
                    int lim2 = lim1 + chunkSize - 1;

                    for (int k = 0; k < total_chunks; k++) {
                        assocFilesInfo.setSnptestOutFileFinalStatus(tt, j, chromo, lim1, lim2, chunkSize, finalStatus);
                        assocFilesInfo.setSummaryFileFinalStatus(tt, j, chromo, lim1, lim2, chunkSize, finalStatus);

                        lim1 = lim1 + chunkSize;
                        lim2 = lim2 + chunkSize;
                    }
                    /*
                     * int index1=0; int index2=1; int last_index=assocFilesInfo.getTheLastReducedFileIndex(j,chromo);
                     * 
                     * for(int k=1; k<=last_index; k++) { //String theStatus =null; //String theStatusPrev= null;
                     * assocFilesInfo.setReducedFileFinalStatus(j, chromo, k, finalStatus); }
                     */
                }
            }
        }
    }

    /**
     * Method to print the current status of each stage (0: unactive, 1:active) of Guidance workflow.
     * 
     * @param parsingArgs
     */
    private static void printStagesStatus(ParseCmdLine parsingArgs) {
        // Verify the status of each stage:
        System.out.println("[Guidance] Current Status of each stage of the whole workflow:");
        System.out.println("[Guidance] convertFromBedToBed      " + parsingArgs.getStageStatus("convertFromBedToBed"));
        System.out.println("[Guidance] createRsIdList           " + parsingArgs.getStageStatus("createRsIdList"));
        System.out.println("[Guidance] phasingBed               " + parsingArgs.getStageStatus("phasingBed"));
        System.out.println("[Guidance] phasing                  " + parsingArgs.getStageStatus("phasing"));
        System.out.println("[Guidance] createListOfExcludedSnps " + parsingArgs.getStageStatus("createListOfExcludedSnps"));
        System.out.println("[Guidance] filterHaplotypes         " + parsingArgs.getStageStatus("filterHaplotypes"));
        System.out.println("[Guidance] imputeWithImpute         " + parsingArgs.getStageStatus("imputeWithImpute"));
        System.out.println("[Guidance] imputeWithMinimac        " + parsingArgs.getStageStatus("imputeWithMinimac"));
        System.out.println("[Guidance] filterByInfo             " + parsingArgs.getStageStatus("filterByInfo"));
        System.out.println("[Guidance] qctoolS                  " + parsingArgs.getStageStatus("qctoolS"));
        System.out.println("[Guidance] snptest                  " + parsingArgs.getStageStatus("snptest"));
        System.out.println("[Guidance] collectSummary           " + parsingArgs.getStageStatus("collectSummary"));
        System.out.println("[Guidance] mergeTwoChunks           " + parsingArgs.getStageStatus("mergeTwoChunks"));
        System.out.println("[Guidance] filterByAll              " + parsingArgs.getStageStatus("filterByAll"));
        System.out.println("[Guidance] jointCondensedFiles      " + parsingArgs.getStageStatus("jointCondensedFiles"));
        System.out.println("[Guidance] jointFilteredByAllFiles  " + parsingArgs.getStageStatus("jointFilteredByAllFiles"));
        System.out.println("[Guidance] generateTopHits          " + parsingArgs.getStageStatus("generateTopHits"));
        System.out.println("[Guidance] generateQQManhattanPlots " + parsingArgs.getStageStatus("generateQQManhattanPlots"));
        System.out.println("[Guidance] combinePanelsComplex     " + parsingArgs.getStageStatus("combinePanelsComplex"));
        System.out.println("[Guidance] combineCondensedFiles    " + parsingArgs.getStageStatus("combineCondensedFiles"));
        System.out.println("[Guidance] initPhenoMatrix          " + parsingArgs.getStageStatus("initPhenoMatrix"));
        System.out.println("[Guidance] addToPhenoMatrix         " + parsingArgs.getStageStatus("addToPhenoMatrix"));
        System.out.println("[Guidance] filloutPhenoMatrix       " + parsingArgs.getStageStatus("filloutPhenoMatrix"));
        System.out.println("[Guidance] finalizePhenoMatrix      " + parsingArgs.getStageStatus("finalizePhenoMatrix"));
        System.out.println("[Guidance] taskx                    " + parsingArgs.getStageStatus("taskx"));
        System.out.println("[Guidance] tasky                    " + parsingArgs.getStageStatus("tasky"));
        System.out.println("[Guidance] taskz                    " + parsingArgs.getStageStatus("taskz"));
    }

    /**
     * Method to print the general information of Guidance
     * 
     */
    private static void printGuidancePackageVersion() {
        System.out.println("[Guidance] *****************************************************************");
        System.out.println("[Guidance] ** This is the Guidance framework to performing imputation,    **");
        System.out.println("[Guidance] ** GWAS and Phenotype analysis of large scale GWAS datasets.   **");
        System.out.println("[Guidance] ** Version: " + PACKAGE_VERSION + "                                   **");
        System.out.println("[Guidance] ** Date release: 20-Jul-2016                                   **");
        System.out.println("[Guidance] ** Contact: http://cg.bsc.es/guidance                          **");
        System.out.println("[Guidance] ******************************************************************\n");
    }

    /**
     * Method to verify that all the environment variables have been well defined
     * 
     */
    private static void verifyEnvVar() throws GuidanceEnvironmentException {
        verify(PLINK_BINARY);
        verify(GTOOL_BINARY);

        verifyEnvVarDefined(R_SCRIPT_BIN_DIR);
        verifyEnvVarDefined(R_SCRIPT_DIR);

        verify(QCTOOL_BINARY);
        verify(SHAPEIT_BINARY);
        verify(IMPUTE2_BINARY);
        verify(SNPTEST_BINARY);

        verifyEnvVarDefined(JAVA_HOME);
    }

    /**
     * Verifies that the given environment variable is correctly defined
     * 
     * @param envVar
     * @throws GuidanceEnvironmentException
     */
    private static void verifyEnvVarDefined(String envVar) throws GuidanceEnvironmentException {
        if (envVar == null) {
            throw new GuidanceEnvironmentException(
                    "[Guidance] Error, " + envVar + " environment variable in not present in .bashrc. You must define it properly");
        }
    }

    /**
     * Verifies that the given environment variable is correctly defined and points to a valid directory
     * 
     * @param envVar
     * @throws GuidanceEnvironmentException
     */
    private static void verify(String envVar) throws GuidanceEnvironmentException {
        verifyEnvVarDefined(envVar);

        File f = new File(envVar);
        if (!f.exists() || f.isDirectory()) {
            throw new GuidanceEnvironmentException(
                    "[Guidance] Error, " + envVar + " does not exist or it is not a binary file. Please check your .bashrc");
        }
    }

}
