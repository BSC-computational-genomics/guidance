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

package guidance.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;

public class ParseCmdLine {

	// Logger
	private static final Logger LOGGER = LogManager.getLogger("Console");

	// Error messages
	private static final String CLASS_HEADER = "[ParseCmdLine.java]";
	private static final String ERROR_PARAM_ORDER = CLASS_HEADER + " Error in the order of parameters, in parameter: ";
	private static final String ERROR_SYNTAX = CLASS_HEADER + " Error of sintax in ";
	private static final String ERROR_SYNTAX_SUFFIX = ", in parameter: ";

	// Chromosome sizes
	private static final int MINIMUMCHUNKSIZE = 1_000;
	private static final int MAX_NUMBER_OF_CHROMOSOMES = 23;
	private static final String[] validManhattans = { "add", "dom", "rec", "gen", "het" };

	private String gwasConfigFile = null;
	private ArrayList<String> argumentsArray = new ArrayList<>();

	private String mixedCohort = null;
	private String mixedBedDir = null;
	private String mixedFamFileName = null;
	private String mixedBimFileName = null;
	private String mixedBedFileName = null;

	private String mixedChrDir = null;

	private String mixedSampleDir = null;
	private String mixedSampleFileName = null;
	private String mixedSampleFile = null;

	private String gmapDir = null;
	private ArrayList<String> gmapFileName = new ArrayList<>();

	private String exclCgatSnp = null;
	private String exclSVSnp = "NO";
	private String imputationTool = null;
	private String phasingTool = null;
	private String[] manhattans = null;

	// chunkSize is always rewritten below.
	private int chunkSize;

	private int refPanelNumber = 0;
	// By default we do not combine panels
	private boolean refPanelCombine = false;
	private ArrayList<String> rpanelTypes = new ArrayList<>();
	private ArrayList<String> rpanelMemory = new ArrayList<>();
	private ArrayList<String> rpanelDir = new ArrayList<>();

	private static String HIGH = "HIGH";
	private static String MEDIUM = "MEDIUM";
	private static String LOW = "LOW";

	private ArrayList<ArrayList<String>> rpanelHapFileName = new ArrayList<>();
	private ArrayList<ArrayList<String>> rpanelLegFileName = new ArrayList<>();
	private ArrayList<ArrayList<String>> rpanelVCFFileName = new ArrayList<>();
	private ArrayList<ArrayList<String>> rpanelHap23FileName = new ArrayList<>();
	private ArrayList<ArrayList<String>> rpanelLeg23FileName = new ArrayList<>();

	// testTypes will be organized as follows:
	// Each string will have: ["test_type_name","response_variable", "covariables"]
	// covariables will be a string like: "se,sex,bmi". That is values separated by
	// coloms.
	private ArrayList<String> testTypesNames = new ArrayList<>();
	private ArrayList<String> responseVars = new ArrayList<>();
	private ArrayList<String> covariables = new ArrayList<>();

	// String rpanelTypes = "";
	private String outDir = null;
	private int start = 0;
	private int end = 0;
	private int endNormal = 0;

	private String wfDeepRequired = null;
	private HashMap<String, Integer> wfPossibleDeeps = new HashMap<>();
	private HashMap<String, Integer> wfAllStages = new HashMap<>();

	private Double mafThreshold = 0.000;
	private Double imputeThreshold = 0.000;
	private Double minimacThreshold = 0.000;
	private Double pvaThreshold = 0.000;
	private Double hweCohortThreshold = 0.000;
	private Double hweCasesThreshold = 0.000;
	private Double hweControlsThreshold = 0.000;

	private String listOfStagesFile = "list_of_stages_default.txt";
	private String removeTemporalFiles = "NO";
	private String compressFiles = "NO";

	private String inputFormat = null;

	private String allCovariables = null;
	private String allResponseVar = null;

	/**
	 * Parse CMD Args into internal values
	 * 
	 * @param args
	 */
	public ParseCmdLine(String[] args) {
		System.out.println("Parsing config file");
		final Integer EMPTY_MASK = 0x0000000;
		wfPossibleDeeps.put("until_convertFromBedToBed", EMPTY_MASK);
		wfPossibleDeeps.put("until_phasing", EMPTY_MASK);
		wfPossibleDeeps.put("until_imputation", EMPTY_MASK);
		wfPossibleDeeps.put("until_qctools", EMPTY_MASK);
		wfPossibleDeeps.put("until_association", EMPTY_MASK);
		wfPossibleDeeps.put("until_filterByAll", EMPTY_MASK);
		wfPossibleDeeps.put("until_summary", EMPTY_MASK);
		wfPossibleDeeps.put("whole_workflow", EMPTY_MASK);
		wfPossibleDeeps.put("from_phasing", EMPTY_MASK);
		wfPossibleDeeps.put("from_phasing_to_summary", EMPTY_MASK);
		wfPossibleDeeps.put("from_phasing_to_filterByAll", EMPTY_MASK);
		wfPossibleDeeps.put("from_phasing_to_qctools", EMPTY_MASK);
		wfPossibleDeeps.put("from_phasing_to_association", EMPTY_MASK);
		wfPossibleDeeps.put("from_phasing_to_imputation", EMPTY_MASK);
		wfPossibleDeeps.put("from_imputation", EMPTY_MASK);
		wfPossibleDeeps.put("from_imputation_to_summary", EMPTY_MASK);
		wfPossibleDeeps.put("from_imputation_to_filterByAll", EMPTY_MASK);
		wfPossibleDeeps.put("from_imputation_to_association", EMPTY_MASK);
		wfPossibleDeeps.put("from_imputation_to_filterByInfo", EMPTY_MASK);
		wfPossibleDeeps.put("from_filterByInfo_to_qctoolS", EMPTY_MASK);
		wfPossibleDeeps.put("from_qctoolS_to_association", EMPTY_MASK);
		wfPossibleDeeps.put("from_association", EMPTY_MASK);
		wfPossibleDeeps.put("from_association_to_filterByAll", EMPTY_MASK);
		wfPossibleDeeps.put("from_association_to_summary", EMPTY_MASK);
		wfPossibleDeeps.put("from_filterByAll", EMPTY_MASK);
		wfPossibleDeeps.put("from_jointFiltered_to_condensed", EMPTY_MASK);
		wfPossibleDeeps.put("from_filterByAll_to_summary", EMPTY_MASK);
		wfPossibleDeeps.put("from_manhattan_to_combine", EMPTY_MASK);
		wfPossibleDeeps.put("from_combine_to_manhattan", EMPTY_MASK);
		wfPossibleDeeps.put("from_combine_to_summary", EMPTY_MASK);
		wfPossibleDeeps.put("from_combine", EMPTY_MASK);
		wfPossibleDeeps.put("from_combineGenManTop_to_summary", EMPTY_MASK);
		wfPossibleDeeps.put("from_summary", EMPTY_MASK);

		// Step 1: We read the file with the configuration and
		// clean the lines from (spaces and comments
		// Then, we put the parameters in an array.
		if ((args.length < 1) && (args.length > 2)) {
			LOGGER.fatal(CLASS_HEADER + " Error in the command line parameters.");
			LOGGER.fatal(CLASS_HEADER + " Usage -1-: -config_file configuration_file.txt");
			System.exit(1);
		}

		if (args[0].equals("-config_file")) {
			gwasConfigFile = args[1];
		}

		try (FileReader fr = new FileReader(gwasConfigFile); BufferedReader br = new BufferedReader(fr);) {
			String line = null;
			while ((line = br.readLine()) != null) {
				char firstChar = line.charAt(0);
				if (firstChar != '#') {
					String myLine = line.replaceAll(" ", "");
					myLine = myLine.replaceAll("\t", "");
					argumentsArray.add(myLine);
				}
				// Process the line
			}
		} catch (IOException ioe) {
			LOGGER.fatal(CLASS_HEADER + " Error opening/reading " + gwasConfigFile);
			LOGGER.fatal(ioe.getMessage());
		}

		// Now, we load the parameters of the execution.
		// There is a strict order in which parameters should be put in the
		// configuration input file.
		// We follow this order.
		int i = 0;
		String tmpArg = argumentsArray.get(i++);
		String[] myArgument = tmpArg.split("=");
		if ((myArgument.length > 0) && (myArgument.length < 3)) {
			if (myArgument[0].equals("wfDeep")) {
				wfDeepRequired = myArgument[1];
				boolean validKey = wfPossibleDeeps.containsKey(wfDeepRequired);
				if (!validKey) {
					LOGGER.fatal(CLASS_HEADER + " Error, wfDeep parameter " + wfDeepRequired + " is not accepted");
					LOGGER.fatal(CLASS_HEADER + "        The only accepted values are:");
					for (String key : wfPossibleDeeps.keySet()) {
						LOGGER.fatal(CLASS_HEADER + "- " + key);
					}
					System.exit(1);
				}
			} else {
				LOGGER.fatal(CLASS_HEADER + ERROR_PARAM_ORDER + i);
				System.exit(1);
			}
		} else {
			LOGGER.fatal(CLASS_HEADER + ERROR_SYNTAX + gwasConfigFile + ERROR_SYNTAX_SUFFIX + i);
			System.exit(1);
		}

		int tmpStart = 0;
		int tmpEnd = 0;
		tmpArg = argumentsArray.get(i++);
		myArgument = tmpArg.split("=");
		if ((myArgument.length > 0) && (myArgument.length < 3)) {
			if (myArgument[0].equals("init_chromosome")) {
				tmpStart = Integer.parseInt(myArgument[1]);
				if (tmpStart < 1 || tmpStart > MAX_NUMBER_OF_CHROMOSOMES) {
					LOGGER.fatal(CLASS_HEADER + " Error, init_chromosome = " + tmpStart);
					LOGGER.fatal(CLASS_HEADER + "        It should be: should be: 0<init_chromosome<=23");
					System.exit(1);
				}
			} else {
				LOGGER.fatal(CLASS_HEADER + ERROR_PARAM_ORDER + myArgument[0]);
				System.exit(1);
			}
		} else {
			LOGGER.fatal(CLASS_HEADER + ERROR_SYNTAX + gwasConfigFile + ERROR_SYNTAX_SUFFIX + myArgument[0]);
			System.exit(1);
		}

		tmpArg = argumentsArray.get(i++);
		myArgument = tmpArg.split("=");
		if ((myArgument.length > 0) && (myArgument.length < 3)) {
			if (myArgument[0].equals("end_chromosome")) {
				tmpEnd = Integer.parseInt(myArgument[1]);
				if (tmpEnd < 1 || tmpEnd > MAX_NUMBER_OF_CHROMOSOMES || tmpEnd < tmpStart) {
					LOGGER.fatal(CLASS_HEADER + " Error, end_chromosome = " + tmpEnd);
					LOGGER.fatal(CLASS_HEADER
							+ "        It should be: should be: 0<init_chromosome<23 and >= init_chromosome");
					System.exit(1);
				}
			} else {
				LOGGER.fatal(CLASS_HEADER + ERROR_PARAM_ORDER + myArgument[0]);
				System.exit(1);
			}
		} else {
			LOGGER.fatal(CLASS_HEADER + ERROR_SYNTAX + gwasConfigFile + ERROR_SYNTAX_SUFFIX + myArgument[0]);
			System.exit(1);
		}

		// Here we have to analyse if we have to include the chromosome 23 or not. If we
		// have to include it, then we
		// enable the doChr23 variable.
		start = tmpStart;
		end = tmpEnd;
		for (int counter = start; counter <= end; counter++) {
			if (counter < 23) {
				endNormal = counter;
			}
		}

		tmpArg = argumentsArray.get(i++);
		myArgument = tmpArg.split("=");
		if ((myArgument.length > 0) && (myArgument.length < 3)) {
			if (myArgument[0].equals("maf_threshold")) {
				mafThreshold = Double.parseDouble(myArgument[1]);
				if (mafThreshold < 0) {
					LOGGER.fatal(CLASS_HEADER + " Error, maf_threshold = " + mafThreshold);
					LOGGER.fatal(CLASS_HEADER + "        It should be: should be: > 0");
					System.exit(1);
				}
			} else {
				LOGGER.fatal(CLASS_HEADER + ERROR_PARAM_ORDER + myArgument[0]);
				System.exit(1);
			}
		} else {
			LOGGER.fatal(CLASS_HEADER + ERROR_SYNTAX + gwasConfigFile + ERROR_SYNTAX_SUFFIX + myArgument[0]);
			System.exit(1);
		}

		tmpArg = argumentsArray.get(i++);
		myArgument = tmpArg.split("=");
		if ((myArgument.length > 0) && (myArgument.length < 3)) {
			if (myArgument[0].equals("impute_threshold")) {
				imputeThreshold = Double.parseDouble(myArgument[1]);
				if (imputeThreshold < 0) {
					LOGGER.fatal(CLASS_HEADER + " Error, info_impute_threshold = " + imputeThreshold);
					LOGGER.fatal(CLASS_HEADER + "        It should be: should be: > 0");
					System.exit(1);
				}
			} else {
				LOGGER.fatal(CLASS_HEADER + ERROR_PARAM_ORDER + myArgument[0]);
				System.exit(1);
			}
		} else {
			LOGGER.fatal(CLASS_HEADER + ERROR_SYNTAX + gwasConfigFile + ERROR_SYNTAX_SUFFIX + myArgument[0]);
			System.exit(1);
		}

		tmpArg = argumentsArray.get(i++);
		myArgument = tmpArg.split("=");
		if ((myArgument.length > 0) && (myArgument.length < 3)) {
			if (myArgument[0].equals("minimac_threshold")) {
				minimacThreshold = Double.parseDouble(myArgument[1]);
				if (minimacThreshold < 0) {
					LOGGER.fatal(CLASS_HEADER + " Error, info_minimac_threshold = " + minimacThreshold);
					LOGGER.fatal(CLASS_HEADER + "        It should be: should be: > 0");
					System.exit(1);
				}
			} else {
				LOGGER.fatal(CLASS_HEADER + ERROR_PARAM_ORDER + myArgument[0]);
				System.exit(1);
			}
		} else {
			LOGGER.fatal(CLASS_HEADER + ERROR_SYNTAX + gwasConfigFile + ERROR_SYNTAX_SUFFIX + myArgument[0]);
			System.exit(1);
		}

		tmpArg = argumentsArray.get(i++);
		myArgument = tmpArg.split("=");
		if ((myArgument.length > 0) && (myArgument.length < 3)) {
			if (myArgument[0].equals("pva_threshold")) {
				pvaThreshold = Double.parseDouble(myArgument[1]);
				if (minimacThreshold < 0) {
					LOGGER.fatal(CLASS_HEADER + " Error, pva_threshold = " + pvaThreshold);
					LOGGER.fatal(CLASS_HEADER + "        It should be: should be: > 0");
					System.exit(1);
				}
			} else {
				LOGGER.fatal(CLASS_HEADER + ERROR_PARAM_ORDER + myArgument[0]);
				System.exit(1);
			}
		} else {
			LOGGER.fatal(CLASS_HEADER + ERROR_SYNTAX + gwasConfigFile + ERROR_SYNTAX_SUFFIX + myArgument[0]);
			System.exit(1);
		}

		tmpArg = argumentsArray.get(i++);
		myArgument = tmpArg.split("=");
		if ((myArgument.length > 0) && (myArgument.length < 3)) {
			if (myArgument[0].equals("hwe_cohort_threshold")) {
				hweCohortThreshold = Double.parseDouble(myArgument[1]);
				// if( hweCohortThreshold < 0 ) {
				// LOGGER.fatal(CLASS_HEADER + " Error, hwe_cohort_threshold = " +
				// hweCohortThreshold);
				// LOGGER.fatal(" It should be: should be: > 0");
				// System.exit(1);
				// }
			} else {
				LOGGER.fatal(CLASS_HEADER + ERROR_PARAM_ORDER + myArgument[0]);
				System.exit(1);
			}
		} else {
			LOGGER.fatal(CLASS_HEADER + ERROR_SYNTAX + gwasConfigFile + ERROR_SYNTAX_SUFFIX + myArgument[0]);
			System.exit(1);
		}

		tmpArg = argumentsArray.get(i++);
		myArgument = tmpArg.split("=");
		if ((myArgument.length > 0) && (myArgument.length < 3)) {
			if (myArgument[0].equals("hwe_cases_threshold")) {
				hweCasesThreshold = Double.parseDouble(myArgument[1]);
				// if( hweCasesThreshold < 0 ) {
				// LOGGER.fatal("Error, hwe_cases_threshold = " + hweCasesThreshold);
				// LOGGER.fatal(" It should be: should be: > 0");
				// System.exit(1);
				// }
			} else {
				LOGGER.fatal(CLASS_HEADER + ERROR_PARAM_ORDER + myArgument[0]);
				System.exit(1);
			}
		} else {
			LOGGER.fatal(CLASS_HEADER + ERROR_SYNTAX + gwasConfigFile + ERROR_SYNTAX_SUFFIX + myArgument[0]);
			System.exit(1);
		}

		tmpArg = argumentsArray.get(i++);
		myArgument = tmpArg.split("=");
		if ((myArgument.length > 0) && (myArgument.length < 3)) {
			if (myArgument[0].equals("hwe_controls_threshold")) {
				hweControlsThreshold = Double.parseDouble(myArgument[1]);
				// if( hweControlsThreshold < 0 ) {
				// LOGGER.fatal("Error, hwe_controls_threshold = " + hweControlsThreshold);
				// LOGGER.fatal(" It should be: should be: > 0");
				// System.exit(1);
				// }
			} else {
				LOGGER.fatal(CLASS_HEADER + ERROR_PARAM_ORDER + myArgument[0]);
				System.exit(1);
			}
		} else {
			LOGGER.fatal(CLASS_HEADER + ERROR_SYNTAX + gwasConfigFile + ERROR_SYNTAX_SUFFIX + myArgument[0]);
			System.exit(1);
		}

		tmpArg = argumentsArray.get(i++);
		myArgument = tmpArg.split("=");
		if ((myArgument.length > 0) && (myArgument.length < 3)) {
			if (myArgument[0].equals("exclude_cgat_snps")) {
				exclCgatSnp = myArgument[1].toUpperCase();
				if (!exclCgatSnp.equals("YES") && !exclCgatSnp.equals("NO")) {
					LOGGER.fatal(CLASS_HEADER + " Error, exclude_cgat_snps = " + exclCgatSnp);
					LOGGER.fatal(CLASS_HEADER + "        It should be: should be: YES or NOT");
					System.exit(1);
				}
			} else {
				LOGGER.fatal(CLASS_HEADER + ERROR_PARAM_ORDER + myArgument[0]);
				System.exit(1);
			}
		} else {
			LOGGER.fatal(CLASS_HEADER + ERROR_SYNTAX + gwasConfigFile + ERROR_SYNTAX_SUFFIX + myArgument[0]);
			System.exit(1);
		}

		tmpArg = argumentsArray.get(i++);
		myArgument = tmpArg.split("=");
		if ((myArgument.length > 0) && (myArgument.length < 3)) {
			if (myArgument[0].equals("phasing_tool")) {
				phasingTool = myArgument[1];
			} else {
				LOGGER.fatal(CLASS_HEADER + ERROR_PARAM_ORDER + myArgument[0]);
				System.exit(1);
			}
		} else {
			LOGGER.fatal(CLASS_HEADER + ERROR_SYNTAX + gwasConfigFile + ERROR_SYNTAX_SUFFIX + myArgument[0]);
			System.exit(1);
		}

		tmpArg = argumentsArray.get(i++);
		myArgument = tmpArg.split("=");
		if ((myArgument.length > 0) && (myArgument.length < 3)) {
			if (myArgument[0].equals("imputation_tool")) {
				imputationTool = myArgument[1];
			} else {
				LOGGER.fatal(CLASS_HEADER + ERROR_PARAM_ORDER + myArgument[0]);
				System.exit(1);
			}
		} else {
			LOGGER.fatal(CLASS_HEADER + ERROR_SYNTAX + gwasConfigFile + ERROR_SYNTAX_SUFFIX + myArgument[0]);
			System.exit(1);
		}

		tmpArg = argumentsArray.get(i++);
		myArgument = tmpArg.split("=");
		if ((myArgument.length > 0) && (myArgument.length < 3)) {
			if (myArgument[0].equals("manhattans")) {
				manhattans = myArgument[1].split(",");
				// If there is only chr23, the program will crash when trying to generate the manhattan and qqplot for the other models
				if (start == 23 && end == 23) {
					if (Arrays.asList(manhattans).contains("add")) {
						manhattans = new String[] {"add"};
					} else {
						LOGGER.fatal("## Guidance only supports additive models when computing only the ChrX ##");
						System.exit(1);
					}
				}
			} else {
				LOGGER.fatal(CLASS_HEADER + ERROR_PARAM_ORDER + myArgument[0]);
				System.exit(1);
			}
		} else {
			LOGGER.fatal(CLASS_HEADER + ERROR_SYNTAX + gwasConfigFile + ERROR_SYNTAX_SUFFIX + myArgument[0]);
			System.exit(1);
		}

		tmpArg = argumentsArray.get(i++);
		myArgument = tmpArg.split("=");
		if ((myArgument.length > 0) && (myArgument.length < 3)) {
			if (myArgument[0].equals("test_types")) {
				String tmpTestTypes = myArgument[1];
				String[] tmpTestTypesArray = tmpTestTypes.split(",");
				for (int kk = 0; kk < tmpTestTypesArray.length; kk++) {
					testTypesNames.add(tmpTestTypesArray[kk]);
				}
			} else {
				LOGGER.fatal(CLASS_HEADER + ERROR_PARAM_ORDER + myArgument[0]);
				System.exit(1);
			}
		} else {
			LOGGER.fatal(CLASS_HEADER + ERROR_SYNTAX + gwasConfigFile + ERROR_SYNTAX_SUFFIX + myArgument[0]);
			System.exit(1);
		}

		for (int kk = 0; kk < testTypesNames.size(); kk++) {
			tmpArg = argumentsArray.get(i++);
			myArgument = tmpArg.split("=");
			if ((myArgument.length > 0) && (myArgument.length < 3)) {
				if (myArgument[0].equals(testTypesNames.get(kk))) {
					String[] tmpFields = myArgument[1].split(":");
					responseVars.add(tmpFields[0]);
					covariables.add(tmpFields[1]);
				} else {
					LOGGER.fatal(CLASS_HEADER + ERROR_PARAM_ORDER + myArgument[0]);
					System.exit(1);
				}
			} else {
				LOGGER.fatal(CLASS_HEADER + ERROR_SYNTAX + gwasConfigFile + ERROR_SYNTAX_SUFFIX + myArgument[0]);
				System.exit(1);
			}
		}

		tmpArg = argumentsArray.get(i++);
		myArgument = tmpArg.split("=");
		if ((myArgument.length > 0) && (myArgument.length < 3)) {
			if (myArgument[0].equals("chunk_size_analysis")) {
				chunkSize = Integer.parseInt(myArgument[1]);
				if (chunkSize < MINIMUMCHUNKSIZE) {
					LOGGER.fatal(CLASS_HEADER
							+ " Error, the value for chunk_size_analysis parameter should not be less than "
							+ MINIMUMCHUNKSIZE);
					System.exit(1);
				}
			} else {
				LOGGER.fatal(CLASS_HEADER + ERROR_PARAM_ORDER + myArgument[0]);
				System.exit(1);
			}
		} else {
			LOGGER.fatal(CLASS_HEADER + ERROR_SYNTAX + gwasConfigFile + ERROR_SYNTAX_SUFFIX + myArgument[0]);
			System.exit(1);
		}

		tmpArg = argumentsArray.get(i++);
		myArgument = tmpArg.split("=");
		if ((myArgument.length > 0) && (myArgument.length < 3)) {
			if (myArgument[0].equals("file_name_for_list_of_stages")) {
				listOfStagesFile = myArgument[1];
			} else {
				LOGGER.fatal(CLASS_HEADER + ERROR_PARAM_ORDER + myArgument[0]);
				System.exit(1);
			}
		} else {
			LOGGER.fatal(CLASS_HEADER + ERROR_SYNTAX + gwasConfigFile + ERROR_SYNTAX_SUFFIX + myArgument[0]);
			System.exit(1);
		}

		tmpArg = argumentsArray.get(i++);
		myArgument = tmpArg.split("=");
		if ((myArgument.length > 0) && (myArgument.length < 3)) {
			if (myArgument[0].equals("remove_temporal_files")) {
				removeTemporalFiles = myArgument[1];
			} else {
				LOGGER.fatal(CLASS_HEADER + ERROR_PARAM_ORDER + myArgument[0]);
				System.exit(1);
			}
		} else {
			LOGGER.fatal(CLASS_HEADER + ERROR_SYNTAX + gwasConfigFile + ERROR_SYNTAX_SUFFIX + myArgument[0]);
			System.exit(1);
		}

		tmpArg = argumentsArray.get(i++);
		myArgument = tmpArg.split("=");
		if ((myArgument.length > 0) && (myArgument.length < 3)) {
			if (myArgument[0].equals("compress_files")) {
				compressFiles = myArgument[1];
			} else {
				LOGGER.fatal(CLASS_HEADER + ERROR_PARAM_ORDER + myArgument[0]);
				System.exit(1);
			}
		} else {
			LOGGER.fatal(CLASS_HEADER + ERROR_SYNTAX + gwasConfigFile + ERROR_SYNTAX_SUFFIX + myArgument[0]);
			System.exit(1);
		}

		tmpArg = argumentsArray.get(i++);
		myArgument = tmpArg.split("=");
		if ((myArgument.length > 0) && (myArgument.length < 3)) {
			if (myArgument[0].equals("input_format")) {
				inputFormat = myArgument[1].toUpperCase();
				if (!inputFormat.equals("GEN") && !inputFormat.equals("BED")) {
					LOGGER.fatal(CLASS_HEADER + " Error, input_format is incorrect = " + inputFormat);
					LOGGER.fatal(CLASS_HEADER + "        It should be GEN or BED");
					System.exit(1);
				}
			} else {
				LOGGER.fatal(CLASS_HEADER + ERROR_PARAM_ORDER + myArgument[0]);
				System.exit(1);
			}
		} else {
			LOGGER.fatal(CLASS_HEADER + ERROR_SYNTAX + gwasConfigFile + ERROR_SYNTAX_SUFFIX + myArgument[0]);
			System.exit(1);
		}

		tmpArg = argumentsArray.get(i++);
		myArgument = tmpArg.split("=");
		if ((myArgument.length > 0) && (myArgument.length < 3)) {
			if (myArgument[0].equals("mixed_cohort")) {
				mixedCohort = myArgument[1];
			} else {
				LOGGER.fatal(CLASS_HEADER + ERROR_PARAM_ORDER + myArgument[0]);
				System.exit(1);
			}
		} else {
			LOGGER.fatal(CLASS_HEADER + ERROR_SYNTAX + gwasConfigFile + ERROR_SYNTAX_SUFFIX + myArgument[0]);
			System.exit(1);
		}

		if (inputFormat.equals("BED")) {
			tmpArg = argumentsArray.get(i++);
			myArgument = tmpArg.split("=");
			if ((myArgument.length > 0) && (myArgument.length < 3)) {
				if (myArgument[0].equals("mixed_bed_file_dir")) {
					mixedBedDir = myArgument[1];
					checkExistence(mixedBedDir);
				} else {
					LOGGER.fatal(CLASS_HEADER + ERROR_PARAM_ORDER + myArgument[0]);
					System.exit(1);
				}
			} else {
				LOGGER.fatal(CLASS_HEADER + ERROR_SYNTAX + gwasConfigFile + ERROR_SYNTAX_SUFFIX + myArgument[0]);
				System.exit(1);
			}

			tmpArg = argumentsArray.get(i++);
			myArgument = tmpArg.split("=");
			if ((myArgument.length > 0) && (myArgument.length < 3)) {
				if (myArgument[0].equals("mixed_bed_file")) {
					mixedBedFileName = myArgument[1];
					checkExistence(mixedBedDir + "/" + mixedBedFileName);
				} else {
					LOGGER.fatal(CLASS_HEADER + ERROR_PARAM_ORDER + myArgument[0]);
					System.exit(1);
				}
			} else {
				LOGGER.fatal(CLASS_HEADER + ERROR_SYNTAX + gwasConfigFile + ERROR_SYNTAX_SUFFIX + myArgument[0]);
				System.exit(1);
			}

			tmpArg = argumentsArray.get(i++);
			myArgument = tmpArg.split("=");
			if ((myArgument.length > 0) && (myArgument.length < 3)) {
				if (myArgument[0].equals("mixed_bim_file")) {
					mixedBimFileName = myArgument[1];
					checkExistence(mixedBedDir + "/" + mixedBimFileName);
				} else {
					LOGGER.fatal(CLASS_HEADER + ERROR_PARAM_ORDER + myArgument[0]);
					System.exit(1);
				}
			} else {
				LOGGER.fatal(CLASS_HEADER + ERROR_SYNTAX + gwasConfigFile + ERROR_SYNTAX_SUFFIX + myArgument[0]);
				System.exit(1);
			}
			tmpArg = argumentsArray.get(i++);
			myArgument = tmpArg.split("=");
			if ((myArgument.length > 0) && (myArgument.length < 3)) {
				if (myArgument[0].equals("mixed_fam_file")) {
					mixedFamFileName = myArgument[1];
					checkExistence(mixedBedDir + "/" + mixedFamFileName);
				} else {
					LOGGER.fatal(CLASS_HEADER + ERROR_PARAM_ORDER + myArgument[0]);
					System.exit(1);
				}
			} else {
				LOGGER.fatal(CLASS_HEADER + ERROR_SYNTAX + gwasConfigFile + ERROR_SYNTAX_SUFFIX + myArgument[0]);
				System.exit(1);
			}
		} else {
			LOGGER.fatal(CLASS_HEADER + " Error of in " + gwasConfigFile + ", the " + inputFormat
					+ " input format is not supported");
			System.exit(1);
		}

		tmpArg = argumentsArray.get(i++);
		myArgument = tmpArg.split("=");
		if ((myArgument.length > 0) && (myArgument.length < 3)) {
			if (myArgument[0].equals("mixed_sample_file_dir")) {
				mixedSampleDir = myArgument[1];
				checkExistence(mixedSampleDir);
			} else {
				LOGGER.fatal(CLASS_HEADER + ERROR_PARAM_ORDER + myArgument[0]);
				System.exit(1);
			}
		} else {
			LOGGER.fatal(CLASS_HEADER + ERROR_SYNTAX + gwasConfigFile + ERROR_SYNTAX_SUFFIX + myArgument[0]);
			System.exit(1);
		}

		tmpArg = argumentsArray.get(i++);
		myArgument = tmpArg.split("=");
		if ((myArgument.length > 0) && (myArgument.length < 3)) {
			String tmpfile = "mixed_sample_file";
			if (myArgument[0].equals(tmpfile)) {
				mixedSampleFileName = myArgument[1];
				checkExistence(mixedSampleDir + File.separator + mixedSampleFileName);
			} else {
				LOGGER.fatal(CLASS_HEADER + ERROR_PARAM_ORDER + myArgument[0]);
				System.exit(1);
			}
		} else {
			LOGGER.fatal(CLASS_HEADER + ERROR_SYNTAX + gwasConfigFile + ERROR_SYNTAX_SUFFIX + myArgument[0]);
			System.exit(1);
		}

		tmpArg = argumentsArray.get(i++);
		myArgument = tmpArg.split("=");
		if ((myArgument.length > 0) && (myArgument.length < 3)) {
			if (myArgument[0].equals("genmap_file_dir")) {
				gmapDir = myArgument[1];
				checkExistence(gmapDir);
			} else {
				LOGGER.fatal(CLASS_HEADER + ERROR_PARAM_ORDER + myArgument[0]);
				System.exit(1);
			}
		} else {
			LOGGER.fatal(CLASS_HEADER + ERROR_SYNTAX + gwasConfigFile + ERROR_SYNTAX_SUFFIX + myArgument[0]);
			System.exit(1);
		}

		if (phasingTool.equals("shapeit")) {
			for (int kk = start; kk <= end; kk++) {
				tmpArg = argumentsArray.get(i++);
				myArgument = tmpArg.split("=");
				if ((myArgument.length > 0) && (myArgument.length < 3)) {
					String chromo = Integer.toString(kk);
					String tmpfile = "genmap_file_chr_" + chromo;
					if (myArgument[0].equals(tmpfile)) {
						gmapFileName.add(myArgument[1]);
						checkExistence(gmapDir + "/" + myArgument[1]);
					} else {
						LOGGER.fatal(CLASS_HEADER + ERROR_PARAM_ORDER + myArgument[0] + " with expected value " + tmpfile);
						System.exit(1);
					}
				} else {
					LOGGER.fatal(CLASS_HEADER + ERROR_SYNTAX + gwasConfigFile + ERROR_SYNTAX_SUFFIX + myArgument[0]);
					System.exit(1);
				}
			}

		} else if (phasingTool.equals("eagle")) {
			tmpArg = argumentsArray.get(i++);
			myArgument = tmpArg.split("=");
			if ((myArgument.length > 0) && (myArgument.length < 3)) {
				String tmpfile = "genmap_file";
				if (myArgument[0].equals(tmpfile)) {
					gmapFileName.add(myArgument[1]);
					checkExistence(gmapDir + "/" + myArgument[1]);
				} else {
					LOGGER.fatal(CLASS_HEADER + ERROR_PARAM_ORDER + myArgument[0]);
					System.exit(1);
				}
			} else {
				LOGGER.fatal(CLASS_HEADER + ERROR_SYNTAX + gwasConfigFile + ERROR_SYNTAX_SUFFIX + myArgument[0]);
				System.exit(1);
			}

		}

		tmpArg = argumentsArray.get(i++);
		myArgument = tmpArg.split("=");
		if ((myArgument.length > 0) && (myArgument.length < 3)) {
			if (myArgument[0].equals("refpanel_number")) {
				refPanelNumber = Integer.parseInt(myArgument[1]);
				if (refPanelNumber < 1) {
					LOGGER.fatal(CLASS_HEADER + " Error, refpanel_number = " + refPanelNumber);
					LOGGER.fatal(CLASS_HEADER + "        It should be: should be: >0");
					System.exit(1);
				}
			} else {
				LOGGER.fatal(CLASS_HEADER + ERROR_PARAM_ORDER + myArgument[0]);
				System.exit(1);
			}
		} else {
			LOGGER.fatal(CLASS_HEADER + ERROR_SYNTAX + gwasConfigFile + ERROR_SYNTAX_SUFFIX + myArgument[0]);
			System.exit(1);
		}

		tmpArg = argumentsArray.get(i++);
		myArgument = tmpArg.split("=");
		if ((myArgument.length > 0) && (myArgument.length < 3)) {
			if (myArgument[0].equals("refpanel_combine")) {
				String myTempCombine = myArgument[1];
				myTempCombine = myTempCombine.toUpperCase();
				if (!myTempCombine.equals("YES") && !myTempCombine.equals("NO")) {
					LOGGER.fatal(CLASS_HEADER + " Error, refpanel_combine = " + myTempCombine);
					LOGGER.fatal(CLASS_HEADER + "        It should be: YES/NO");
					System.exit(1);
				}
				// Now, if myTempCombine is "YES" and the refPanelNumber > 1, then
				// refPanelCombine = true
				if (myTempCombine.equals("YES") && refPanelNumber > 1) {
					refPanelCombine = true;
				} else {
					refPanelCombine = false;
				}
			} else {
				LOGGER.fatal(CLASS_HEADER + ERROR_PARAM_ORDER + myArgument[0]);
				System.exit(1);
			}
		} else {
			LOGGER.fatal(CLASS_HEADER + ERROR_SYNTAX + gwasConfigFile + ERROR_SYNTAX_SUFFIX + myArgument[0]);
			System.exit(1);
		}

		for (int kk = 0; kk < refPanelNumber; kk++) {
			tmpArg = argumentsArray.get(i++);
			myArgument = tmpArg.split("=");
			if ((myArgument.length > 0) && (myArgument.length < 3)) {
				if (myArgument[0].equals("refpanel_type")) {
					rpanelTypes.add(myArgument[1]);
				} else {
					LOGGER.fatal(CLASS_HEADER + ERROR_PARAM_ORDER + myArgument[0]);
					System.exit(1);
				}
			} else {
				LOGGER.fatal(CLASS_HEADER + ERROR_SYNTAX + gwasConfigFile + ERROR_SYNTAX_SUFFIX + myArgument[0]);
				System.exit(1);
			}

			tmpArg = argumentsArray.get(i++);
			myArgument = tmpArg.split("=");
			if ((myArgument.length > 0) && (myArgument.length < 3)) {
				if (myArgument[0].equals("refpanel_memory")) {
					String panelMemory = myArgument[1].toUpperCase();
					if (panelMemory.equals(HIGH) || panelMemory.equals(MEDIUM) || panelMemory.equals(LOW)) {
						rpanelMemory.add(panelMemory);
					} else {
						LOGGER.fatal(
								CLASS_HEADER + ERROR_SYNTAX + "the amount of memory " + panelMemory + " introduced");
						System.exit(1);
					}
				} else {
					LOGGER.fatal(CLASS_HEADER + ERROR_PARAM_ORDER + myArgument[0]);
					System.exit(1);
				}
			} else {
				LOGGER.fatal(CLASS_HEADER + ERROR_SYNTAX + gwasConfigFile + ERROR_SYNTAX_SUFFIX + myArgument[0]);
				System.exit(1);
			}

			tmpArg = argumentsArray.get(i++);
			myArgument = tmpArg.split("=");
			if ((myArgument.length > 0) && (myArgument.length < 3)) {
				if (myArgument[0].equals("refpanel_file_dir")) {
					rpanelDir.add(myArgument[1]);
					checkExistence(myArgument[1]);
					String tmpRpanelDir = myArgument[1];

					if (imputationTool.equals("impute")) {

						ArrayList<String> chromoListRpanelHapFileName = new ArrayList<>();
						for (int j = start; j <= end; j++) {
							tmpArg = argumentsArray.get(i++);
							myArgument = tmpArg.split("=");
							if ((myArgument.length > 0) && (myArgument.length < 3)) {
								String chromo = Integer.toString(j);
								String tmpfile = "refpanel_hap_file_chr_" + chromo;
								if (myArgument[0].equals(tmpfile)) {
									chromoListRpanelHapFileName.add(myArgument[1]);
									checkExistence(tmpRpanelDir + File.separator + myArgument[1]);
								} else {
									LOGGER.fatal(CLASS_HEADER + ERROR_PARAM_ORDER + myArgument[0]);
									System.exit(1);
								}
							} else {
								LOGGER.fatal(CLASS_HEADER + ERROR_SYNTAX + gwasConfigFile + ERROR_SYNTAX_SUFFIX
										+ myArgument[0]);
								System.exit(1);
							}
						}
						rpanelHapFileName.add(chromoListRpanelHapFileName);

						// exclSVSnp = "NO";
						LOGGER.info(CLASS_HEADER + " We are going to use 'impute' tool for imputation stage... ");
						ArrayList<String> chromoListRpanelLegFileName = new ArrayList<>();
						for (int j = start; j <= end; j++) {
							tmpArg = argumentsArray.get(i++);
							myArgument = tmpArg.split("=");
							if ((myArgument.length > 0) && (myArgument.length < 3)) {
								String chromo = Integer.toString(j);
								String tmpfile = "refpanel_leg_file_chr_" + chromo;
								if (myArgument[0].equals(tmpfile)) {
									chromoListRpanelLegFileName.add(myArgument[1]);
									checkExistence(tmpRpanelDir + File.separator + myArgument[1]);
								} else {
									LOGGER.fatal(CLASS_HEADER + ERROR_PARAM_ORDER + myArgument[0]);
									System.exit(1);
								}
							} else {
								LOGGER.fatal(CLASS_HEADER + ERROR_SYNTAX + gwasConfigFile + ERROR_SYNTAX_SUFFIX
										+ myArgument[0]);
								System.exit(1);
							}
						}
						rpanelLegFileName.add(chromoListRpanelLegFileName);
					} else if (imputationTool.equals("minimac")) {
						// exclSVSnp = "YES";
						LOGGER.fatal("[ParseCmdLine] We are going to use 'minimac' tool for imputation stage... ");
						ArrayList<String> chromoListRpanelVCFFileName = new ArrayList<String>();
						int endVCF = end;
						if (endVCF == 23) {
							endVCF = 22;
						}
						for (int j = start; j <= endVCF; j++) {
							tmpArg = argumentsArray.get(i++);
							myArgument = tmpArg.split("=");
							if ((myArgument.length > 0) && (myArgument.length < 3)) {
								String chromo = Integer.toString(j);
								String tmpfile = "refpanel_vcf_file_chr_" + chromo;
								if (myArgument[0].equals(tmpfile)) {
									chromoListRpanelVCFFileName.add(myArgument[1]);
									checkExistence(tmpRpanelDir + "/" + myArgument[1]);
								} else {
									LOGGER.fatal(ERROR_PARAM_ORDER + myArgument[0]);
									System.exit(1);
								}
							} else {
								LOGGER.fatal("[ParseCmdLine.java] Error of sintax in " + gwasConfigFile
										+ ", in parameter: " + myArgument[0]);
								System.exit(1);
							}
						}
						rpanelVCFFileName.add(chromoListRpanelVCFFileName);
						if (end == 23) {
							tmpArg = argumentsArray.get(i++);
							myArgument = tmpArg.split("=");
							ArrayList<String> chromoListRpanelHapFileName = new ArrayList<String>();
							if ((myArgument.length > 0) && (myArgument.length < 3)) {
								String chromo = "23";
								String tmpfile = "refpanel_hap_file_chr_" + chromo;
								if (myArgument[0].equals(tmpfile)) {
									chromoListRpanelHapFileName.add(myArgument[1]);
									checkExistence(myArgument[1]);
								} else {
									LOGGER.fatal(ERROR_PARAM_ORDER + myArgument[0]);
									System.exit(1);
								}
							} else {
								LOGGER.fatal("[ParseCmdLine.java] Error of sintax in " + gwasConfigFile
										+ ", in parameter: " + myArgument[0]);
								System.exit(1);
							}
							tmpArg = argumentsArray.get(i++);
							myArgument = tmpArg.split("=");
							ArrayList<String> chromoListRpanelLegFileName = new ArrayList<String>();
							if ((myArgument.length > 0) && (myArgument.length < 3)) {
								String chromo = "23";
								String tmpfile = "refpanel_leg_file_chr_" + chromo;
								if (myArgument[0].equals(tmpfile)) {
									chromoListRpanelLegFileName.add(myArgument[1]);
									checkExistence(myArgument[1]);
								} else {
									LOGGER.fatal(ERROR_PARAM_ORDER + myArgument[0]);
									System.exit(1);
								}
							} else {
								LOGGER.fatal("[ParseCmdLine.java] Error of sintax in " + gwasConfigFile
										+ ", in parameter: " + myArgument[0]);
								System.exit(1);
							}
							rpanelHap23FileName.add(chromoListRpanelHapFileName);
							rpanelLeg23FileName.add(chromoListRpanelLegFileName);
							rpanelHapFileName.add(chromoListRpanelHapFileName);
							rpanelLegFileName.add(chromoListRpanelLegFileName);
						}
						LOGGER.info(CLASS_HEADER + " We are going to use 'minimac' tool for imputation stage... ");
					} else {
						LOGGER.fatal(CLASS_HEADER
								+ " Sorry, Only 'impute' or 'minimac' Tools are supported right now for the imputation process... ");
						LOGGER.fatal(CLASS_HEADER + " Future version will support other tools... ");
						System.exit(1);
					}
				} else {
					LOGGER.fatal(CLASS_HEADER + ERROR_PARAM_ORDER + myArgument[0]);
					System.exit(1);
				}
			} else {
				LOGGER.fatal(CLASS_HEADER + ERROR_SYNTAX + gwasConfigFile + ERROR_SYNTAX_SUFFIX + myArgument[0]);
				System.exit(1);
			}
		}
		tmpArg = argumentsArray.get(i++);
		myArgument = tmpArg.split("=");
		if ((myArgument.length > 0) && (myArgument.length < 3)) {
			if (myArgument[0].equals("outputdir")) {
				outDir = myArgument[1];
				checkExistence(outDir);
			} else {
				LOGGER.fatal(CLASS_HEADER + ERROR_PARAM_ORDER + myArgument[0]);
				System.exit(1);
			}
		} else {
			LOGGER.fatal(CLASS_HEADER + ERROR_SYNTAX + gwasConfigFile + ERROR_SYNTAX_SUFFIX + myArgument[0]);
			System.exit(1);
		}

		// printInputCmd(inputFormat);

		// Finally we define which combination of GWAS analysis we are doing:
		// Options are: MIXED with GEN files
		// MIXED with BED files
		// SEPARATED with GEN files
		// SEPARATED with BED files

		// Now, depending on the wfDeepRequired, we have to activate the corresponding
		// stages.
		activateStages(wfDeepRequired, imputationTool);

	} // End of ParseCmdLine class

	/**
	 * Method to get the workflow deep the user wants to run
	 * 
	 * @return
	 */
	public String getWfDeepRequired() {
		return this.wfDeepRequired;
	}

	/**
	 * Method to get the workflow deep value the user wants to run
	 * 
	 * @param wfDeepRequired
	 * @return
	 */
	public int getWfDeepValue(String wfDeepRequired) {
		return this.wfPossibleDeeps.get(wfDeepRequired);
	}

	/**
	 * Method to get start gen information
	 * 
	 * @return
	 */
	public int getStart() {
		return this.start;
	}

	/**
	 * Method to get end gen information
	 * 
	 * @return
	 */
	public int getEnd() {
		return this.end;
	}

	/**
	 * Method to get end gen information
	 * 
	 * @return
	 */
	public int getEndNormal() {
		return this.endNormal;
	}

	/**
	 * Method to get mafThreshold flag
	 * 
	 * @return
	 */
	public Double getMafThreshold() {
		return this.mafThreshold;
	}

	/**
	 * Method to get infoThreshold flag when using impute
	 * 
	 * @return
	 */
	public Double getImputeThreshold() {
		return this.imputeThreshold;
	}

	/**
	 * Method to get infoThreshold flag when using minimac
	 * 
	 * @return
	 */
	public Double getMinimacThreshold() {
		return this.minimacThreshold;
	}

	/**
	 * Method to get infoThreshold flag
	 * 
	 * @return
	 */
	public Double getPvaThreshold() {
		return this.pvaThreshold;
	}

	/**
	 * Method to get hweThreshold flag
	 * 
	 * @return
	 */
	public Double getHweCohortThreshold() {
		return this.hweCohortThreshold;
	}

	/**
	 * Method to get hweCasesThreshold flag
	 * 
	 * @return
	 */
	public Double getHweCasesThreshold() {
		return this.hweCasesThreshold;
	}

	/**
	 * Method to get hweControlshreshold flag
	 * 
	 * @return
	 */
	public Double getHweControlsThreshold() {
		return this.hweControlsThreshold;
	}

	/**
	 * Method to get exclCgatSnp flag
	 * 
	 * @return
	 */
	public String getExclCgatSnp() {
		return this.exclCgatSnp;
	}

	/**
	 * Method to get exclSVSnp flag
	 * 
	 * @return
	 */
	public String getExclSVSnp() {
		return this.exclSVSnp;
	}

	/**
	 * Method to get phasingTool flag
	 * 
	 * @return
	 */
	public String getPhasingTool() {
		return this.phasingTool;
	}

	/**
	 * Method to get imputationTool flag
	 * 
	 * @return
	 */
	public String getImputationTool() {
		return this.imputationTool;
	}

	/**
	 * Method to get testTypesNames flag
	 * 
	 * @param testNameIndex
	 * @return
	 */
	public String getTestTypeName(int testNameIndex) {
		return this.testTypesNames.get(testNameIndex);
	}

	/**
	 * Method to get the number of testTypes
	 * 
	 * @return
	 */
	public int getNumberOfTestTypeName() {
		return this.testTypesNames.size();
	}

	/**
	 * Method to get the name of covariables to be included in the snptest
	 * 
	 * @param testNameIndex
	 * @return
	 */
	public String getCovariables(int testNameIndex) {
		return this.covariables.get(testNameIndex);
	}

	private String fromVectorToUnique(ArrayList<String> vector) {
		HashSet<String> uniqueCovars = new HashSet<String>();
		for (String stringWithElements : vector) {
			List<String> listWithElements = Arrays.asList(stringWithElements.split(","));
			for (String elem : listWithElements) {
				uniqueCovars.add(elem);
			}
		}
		ArrayList<String> uniqueElementsArray = new ArrayList<String>(uniqueCovars);
		String stringWithUniqueValues = uniqueElementsArray.get(0);
		for (int i = 1; i < uniqueElementsArray.size(); ++i) {
			stringWithUniqueValues = stringWithUniqueValues + ("," + uniqueElementsArray.get(i));
		}
		return stringWithUniqueValues;
	}

	/**
	 * Method to get the name of all the covariables considered
	 * 
	 * @param testNameIndex
	 * @return
	 */
	public String getAllCovariables() {
		if (allCovariables == null) {
			allCovariables = fromVectorToUnique(this.covariables);
		}
		return allCovariables;
	}

	/**
	 * Method to get the name of all the response vars considered
	 * 
	 * @param testNameIndex
	 * @return
	 */
	public String getAllResponseVars() {
		if (allResponseVar == null) {
			allResponseVar = fromVectorToUnique(this.responseVars);
		}
		return allResponseVar;
	}

	/**
	 * Method to get the name of responseVars to included in the snptest
	 * 
	 * @param testNameIndex
	 * @return
	 */
	public String getResponseVar(int testNameIndex) {
		return this.responseVars.get(testNameIndex);
	}

	/**
	 * Method to get the chunk size to be used in the analysis
	 * 
	 * @return
	 */
	public int getChunkSize() {
		return this.chunkSize;
	}

	/**
	 * Method to get the list of stages
	 * 
	 * @return
	 */
	public String getListOfStagesFile() {
		return this.listOfStagesFile;
	}

	/**
	 * Method to get the input format
	 * 
	 * @return
	 */
	public String getInputFormat() {
		return this.inputFormat;
	}

	/**
	 * Method to get the value of the removeTemporalFiles variable It can be YES or
	 * NO only
	 * 
	 * @return
	 */
	public String getRemoveTemporalFiles() {
		return this.removeTemporalFiles;
	}

	/**
	 * Method to get the value of the compressFiles variable It can be YES or NO
	 * only
	 * 
	 * @return
	 */
	public String getCompressFiles() {
		return this.compressFiles;
	}

	/**
	 * Method to get bedFile information
	 * 
	 * @return
	 */
	public String getBedFileName() {
		return this.mixedBedFileName;
	}

	/**
	 * Method to get bimFile information
	 * 
	 * @return
	 */
	public String getBimFileName() {
		return this.mixedBimFileName;
	}

	/**
	 * Method to get famFile information
	 * 
	 * @return
	 */
	public String getFamFileName() {
		return this.mixedFamFileName;
	}

	/**
	 * Method to get Cohort information
	 * 
	 * @return
	 */
	public String getCohort() {
		return this.mixedCohort;
	}

	// Cases information
	/**
	 * Method to get the mixedBedDir information
	 * 
	 * @return
	 */
	public String getBedDir() {
		return this.mixedBedDir;
	}

	// Cases information
	/**
	 * Method to get the mixedChrDir information
	 * 
	 * @return
	 */
	public String getChrDir() {
		return this.mixedChrDir;
	}

	/**
	 * Method to get sampleDir information
	 * 
	 * @return
	 */
	public String getSampleDir() {
		return this.mixedSampleDir;
	}

	/**
	 * Method to get the xxxxxSampleFileName
	 * 
	 * @return
	 */
	public String getSampleFileName() {
		return this.mixedSampleFileName;
	}

	/**
	 * Method to get the xxxxxSampleFile
	 * 
	 * @return
	 */
	public String getSampleFile() {
		return this.mixedSampleFile;
	}

	// General information
	/**
	 * Method to get gmapDir information
	 * 
	 * @return
	 */
	public String getGmapDir() {
		return this.gmapDir;
	}

	/**
	 * Method to get the gmapFileName
	 * 
	 * @param chromo
	 * @return
	 */
	public String getGmapFileName(int chromo) {
		int index = chromo - getStart();
		if (index < 0) {
			LOGGER.fatal(CLASS_HEADER + " Error, the chromosome number should be > " + getStart());
			System.exit(1);
			return "none";
		}
		return this.gmapFileName.get(index);
	}

	/**
	 * Method to get the gmapFileName in case we use EAGLE It has to be taken into
	 * account that there is only one file in this case
	 * 
	 * @param chromo
	 * @return
	 */
	public String getGmapFileNameEagle() {
		return gmapFileName.get(0);
	}

	/**
	 * Method to get refPanelNumber information
	 * 
	 * @return
	 */
	public int getRpanelNumber() {
		return this.refPanelNumber;
	}

	/**
	 * Method to get refPanelCombine variable
	 * 
	 * @return
	 */
	public boolean getRefPanelCombine() {
		return this.refPanelCombine;
	}

	/**
	 * Method to get refPanelType information
	 * 
	 * @param indexRpanel
	 * @return
	 */
	public String getRpanelType(int indexRpanel) {
		return this.rpanelTypes.get(indexRpanel);
	}

	/**
	 * Method to get refPanelMemory information
	 * 
	 * @param indexRpanel
	 * @return
	 */
	public String getRpanelMemory(int indexRpanel) {
		return this.rpanelMemory.get(indexRpanel);
	}

	/**
	 * Method to get rpanelDir information
	 * 
	 * @param indexRpanel
	 * @return
	 */
	public String getRpanelDir(int indexRpanel) {
		return this.rpanelDir.get(indexRpanel);

	}

	/**
	 * Method to get ALL the rpanelTypes in an ArrayList
	 * 
	 * @return
	 */
	public ArrayList<String> getRpanelTypes() {
		return this.rpanelTypes;
	}

	/**
	 * Method to get the rPanelHapFileName
	 * 
	 * @param indexRpanel
	 * @param chromo
	 * @return
	 */
	public String getRpanelHapFileName(int indexRpanel, int chromo) {
		int index = chromo - getStart();
		if (chromo < getStart() || chromo > getEnd()) {
			LOGGER.fatal(CLASS_HEADER + " Error, the chromosome number should be " + getStart() + " <= chromo <= "
					+ getEnd());
			System.exit(1);
			return "none";
		}

		if ((indexRpanel < 0) || (indexRpanel > rpanelTypes.size())) {
			LOGGER.fatal(CLASS_HEADER + " Error, the indexRpanel should be  0<= indexRpanel<=" + rpanelTypes.size());
			System.exit(1);
			return "none";
		}

		return this.rpanelHapFileName.get(indexRpanel).get(index);
	}

	/**
	 * Method to get the rPanelLegFileName
	 * 
	 * @param indexRpanel
	 * @param chromo
	 * @return
	 */
	public String getRpanelLegFileName(int indexRpanel, int chromo) {
		int index = chromo - getStart();
		if (chromo < getStart() || chromo > getEnd()) {
			LOGGER.fatal(CLASS_HEADER + " Error, the chromosome number should be " + getStart() + " <= chromo <= "
					+ getEnd());
			System.exit(1);
			return "none";
		}

		if ((indexRpanel < 0) || (indexRpanel > rpanelTypes.size())) {
			LOGGER.fatal(CLASS_HEADER + " Error, the indexRpanel should be  0<= indexRpanel<=" + rpanelTypes.size());
			System.exit(1);
			return "none";
		}

		return this.rpanelLegFileName.get(indexRpanel).get(index);
	}

	/**
	 * Method to get the rpanelVCFFileName
	 * 
	 * @param indexRpanel
	 * @param chromo
	 * @return
	 */
	public String getRpanelVCFFileName(int indexRpanel, int chromo) {
		int index = chromo - getStart();
		if (chromo < getStart() || chromo > getEnd()) {
			System.err.println("[ParseCmdLine.java] Error, the chromosome number should be " + getStart()
					+ " <= chromo <= " + getEnd());
			System.exit(1);
			return "none";
		}

		if ((indexRpanel < 0) || (indexRpanel > rpanelTypes.size())) {
			System.err.println(
					"[ParseCmdLine.java] Error, the indexRpanel should be  0<= indexRpanel<=" + rpanelTypes.size());
			System.exit(1);
			return "none";
		}

		return rpanelVCFFileName.get(indexRpanel).get(index);
	}

	/**
	 * Method to get the rpanelHap23FileName (Minimac)
	 * 
	 * @param indexRpanel
	 * @return
	 */
	public String getRpanelHap23FileName(int indexRpanel) {

		if ((indexRpanel < 0) || (indexRpanel > rpanelTypes.size())) {
			System.err.println(
					"[ParseCmdLine.java] Error, the indexRpanel should be  0<= indexRpanel<=" + rpanelTypes.size());
			System.exit(1);
			return "none";
		}

		return rpanelHap23FileName.get(indexRpanel).get(0);
	}

	/**
	 * Method to get the rpanelLeg23FileName (Minimac)
	 * 
	 * @param indexRpanel
	 * @return
	 */
	public String getRpanelLeg23FileName(int indexRpanel) {

		if ((indexRpanel < 0) || (indexRpanel > rpanelTypes.size())) {
			System.err.println(
					"[ParseCmdLine.java] Error, the indexRpanel should be  0<= indexRpanel<=" + rpanelTypes.size());
			System.exit(1);
			return "none";
		}

		return rpanelLeg23FileName.get(indexRpanel).get(0);
	}

	/**
	 * Method to get the manhattan plots to launch
	 * 
	 */
	public String[] getManhattanOptions() {
		return manhattans;
	}

	/**
	 * Method to get outDir information
	 * 
	 * @return
	 */
	public String getOutDir() {
		return this.outDir;
	}

	/**
	 * Method for printing the input command line
	 * 
	 * @param inputFormat
	 */
	public void printInputCmd(String inputFormat) {
		LOGGER.info("[ParseCmdLine] Execution will be done with the following input parameters:");
		LOGGER.info("init_chromosome              = " + start);
		LOGGER.info("end_chromosome               = " + end);
		LOGGER.info("maf_threshold                = " + mafThreshold);
		LOGGER.info("impute_threshold             = " + imputeThreshold);
		LOGGER.info("minimac_threshold            = " + minimacThreshold);
		LOGGER.info("hwe_cohort_threshold         = " + hweCohortThreshold);
		LOGGER.info("hwe_cases_threshold          = " + hweCasesThreshold);
		LOGGER.info("hwe_controls_threshold       = " + hweControlsThreshold);
		LOGGER.info("exclude_cgat_snps            = " + exclCgatSnp);
		int number_of_tests = testTypesNames.size();

		for (int kk = 0; kk < number_of_tests; kk++) {
			String tmp_test_type = testTypesNames.get(kk);
			String tmp_responseVar = responseVars.get(kk);
			String tmp_covariables = covariables.get(kk);
			LOGGER.info(tmp_test_type + " = " + tmp_responseVar + ":" + tmp_covariables);
		}
		LOGGER.info("phasing_tool              = " + phasingTool);
		LOGGER.info("imputation_tool              = " + imputationTool);
		LOGGER.info("names_of_covariables         = " + covariables);
		LOGGER.info("chunk_size_analysis          = " + chunkSize);
		LOGGER.info("file_name_for_list_of_stages = " + listOfStagesFile);

		LOGGER.info("mixed_cohort                 = " + mixedCohort);

		if (inputFormat.equals("BED")) {
			LOGGER.info("mixed_bed_file_dir           = " + mixedBedDir);
		} else {
			LOGGER.fatal(CLASS_HEADER + " Error, input format " + inputFormat + " is not supported");
			System.exit(1);
		}

		LOGGER.info("genmap_file_dir = " + gmapDir);
		for (int kk = getStart(); kk <= getEnd(); kk++) {
			int index = kk - getStart();
			LOGGER.info("\tgenmap_file_chr_" + kk + " = " + gmapFileName.get(index));
		}

		LOGGER.info("refpanel_number = " + refPanelNumber);
		for (int kk = 0; kk < rpanelTypes.size(); kk++) {
			LOGGER.info("\trefpanel_type = " + rpanelTypes.get(kk));
			LOGGER.info("\trefpanel_file_dir = " + rpanelDir.get(kk));
			for (int jj = getStart(); jj <= getEnd(); jj++) {
				int index = jj - getStart();
				LOGGER.info("\t\trefpanel_hap_file_chr_" + jj + " = " + rpanelHapFileName.get(kk).get(index));
			}
			for (int jj = getStart(); jj <= getEnd(); jj++) {
				int index = jj - getStart();
				LOGGER.info("\t\trefpanel_leg_file_chr_" + jj + " = " + rpanelLegFileName.get(kk).get(index));
			}
		}
		LOGGER.info("\toutputdir = " + outDir);
		LOGGER.info("------------------------------------");
	}

	/**
	 * Method to activate stages of the workflow depending on the wfDeepRequired
	 * string given by the user.
	 * 
	 * @param wfDeepRequired
	 * @param imputationTool
	 */
	public void activateStages(String wfDeepRequired, String imputationTool) {

		// First of all, we activate the correct PossibleDeeps depending on the kind of
		// imputation tool that is used:
		// Important: The order of the bits in the value are related to the order of the
		// stages in the wfAllStages
		// Hastable below. If you are going to modify the list of stages you should fix
		// the new binary value in
		// wfPossibleDeeps.
		if (imputationTool.equals("impute")) {
			wfPossibleDeeps.put("until_convertFromBedToBed", 0x6000000);
			wfPossibleDeeps.put("until_phasing", 0x7800000);
			wfPossibleDeeps.put("until_imputation", 0x7900000);
			wfPossibleDeeps.put("until_qctools", 0x7960000);
			wfPossibleDeeps.put("until_association", 0x7970000);
			wfPossibleDeeps.put("until_filterByAll", 0x797E000);
			wfPossibleDeeps.put("until_summary", 0x797FF80);
			wfPossibleDeeps.put("whole_workflow", 0x797FFC0);
			wfPossibleDeeps.put("from_phasing", 0x017FFC0);
			wfPossibleDeeps.put("from_phasing_to_summary", 0x017FF80);
			wfPossibleDeeps.put("from_phasing_to_filterByAll", 0x017E000);
			wfPossibleDeeps.put("from_phasing_to_qctools", 0x0160000);
			wfPossibleDeeps.put("from_phasing_to_association", 0x0170000);
			wfPossibleDeeps.put("from_phasing_to_imputation", 0x0100000);
			wfPossibleDeeps.put("from_imputation", 0x007FFC0);
			wfPossibleDeeps.put("from_imputation_to_summary", 0x007FF00);
			wfPossibleDeeps.put("from_imputation_to_filterByAll", 0x007E000);
			wfPossibleDeeps.put("from_imputation_to_association", 0x0070000);
			wfPossibleDeeps.put("from_imputation_to_filterByInfo", 0x0040000);
			wfPossibleDeeps.put("from_filterByInfo_to_qctoolS", 0x0020000);
			wfPossibleDeeps.put("from_qctoolS_to_association", 0x0010000);
			wfPossibleDeeps.put("from_association", 0x000FFC0);
			wfPossibleDeeps.put("from_association_to_filterByAll", 0x000E000);
			wfPossibleDeeps.put("from_association_to_summary", 0x000FF80);
			wfPossibleDeeps.put("from_filterByAll", 0x0001FC0);
			wfPossibleDeeps.put("from_jointFiltered_to_condensed", 0x0000800);
			wfPossibleDeeps.put("from_filterByAll_to_summary", 0x0001F80);
			wfPossibleDeeps.put("from_manhattan_to_combine", 0x0001200);
			wfPossibleDeeps.put("from_combine_to_manhattan", 0x0000100);
			wfPossibleDeeps.put("from_combine_to_summary", 0x0000180);
			wfPossibleDeeps.put("from_combine", 0x00001C0);
			wfPossibleDeeps.put("from_combineGenManTop_to_summary", 0x0000080);
			wfPossibleDeeps.put("from_summary", 0x0000040);
		} else if (imputationTool.equals("minimac")) {
			wfPossibleDeeps.put("until_convertFromBedToBed", 0x6000000);
			wfPossibleDeeps.put("until_phasing", 0x7E00000);
			wfPossibleDeeps.put("until_imputation", 0x7E80000);
			wfPossibleDeeps.put("until_qctools", 0x7EE0000);
			wfPossibleDeeps.put("until_association", 0x7EF0000);
			wfPossibleDeeps.put("until_filterByAll", 0x7EFE000);
			wfPossibleDeeps.put("until_summary", 0x7EFFF80);
			wfPossibleDeeps.put("whole_workflow", 0x7EFFFC0);
			wfPossibleDeeps.put("from_phasing", 0x00FFFC0);
			wfPossibleDeeps.put("from_phasing_to_summary", 0x00FFF00);
			wfPossibleDeeps.put("from_phasing_to_filterByAll", 0x00FE000);
			wfPossibleDeeps.put("from_phasing_to_qctools", 0x00E0000);
			wfPossibleDeeps.put("from_phasing_to_association", 0x00F0000);
			wfPossibleDeeps.put("from_phasing_to_imputation", 0x0080000);
			wfPossibleDeeps.put("from_imputation", 0x007FFC0);
			wfPossibleDeeps.put("from_imputation_to_summary", 0x007FF00);
			wfPossibleDeeps.put("from_imputation_to_filterByAll", 0x007E000);
			wfPossibleDeeps.put("from_imputation_to_association", 0x0070000);
			wfPossibleDeeps.put("from_imputation_to_filterByInfo", 0x0040000);
			wfPossibleDeeps.put("from_filterByInfo_to_qctoolS", 0x0020000);
			wfPossibleDeeps.put("from_qctoolS_to_association", 0x0010000);
			wfPossibleDeeps.put("from_association", 0x000FFC0);
			wfPossibleDeeps.put("from_association_to_filterByAll", 0x000E000);
			wfPossibleDeeps.put("from_association_to_summary", 0x000FF80);
			wfPossibleDeeps.put("from_filterByAll", 0x0001FC0);
			wfPossibleDeeps.put("from_jointFiltered_to_condensed", 0x0000800);
			wfPossibleDeeps.put("from_filterByAll_to_summary", 0x0001F80);
			wfPossibleDeeps.put("from_manhattan_to_combine", 0x0001200);
			wfPossibleDeeps.put("from_combine_to_manhattan", 0x0000100);
			wfPossibleDeeps.put("from_combine_to_summary", 0x0000180);
			wfPossibleDeeps.put("from_combine", 0x00001C0);
			wfPossibleDeeps.put("from_combineGenManTop_to_summary", 0x0000080);
			wfPossibleDeeps.put("from_summary", 0x0000040);
		} else {
			LOGGER.fatal(CLASS_HEADER + " Error, the imputation tool: " + imputationTool
					+ " is not supported in this version...");
			System.exit(1);
		}

		String[] steps = { "convertFromBedToBed", "createRsIdList", "phasingBed", "phasing", "createListOfExcludedSnps",
				"filterHaplotypes", "imputeWithImpute", "imputeWithMinimac", "filterByInfo", "qctoolS", "snptest",
				"collectSummary", "mergeTwoChunks", "filterByAll", "jointCondensedFiles", "jointFilteredByAllFiles",
				"generateTopHits", "generateQQManhattanPlots", "combinePanelsComplex", "combGenerateManhattanTop",
				"phenoAnalysis", "tasku", "taskv", "taskw", "taskx", "tasky", "taskz" };

		final Integer MASK1 = 0x00001;

		// Shift 1 and Mask1
		for (int stageNumber = 0; stageNumber < steps.length; ++stageNumber) {
			int tmpVar = (wfPossibleDeeps.get(wfDeepRequired) >> stageNumber) & MASK1;
			wfAllStages.put(steps[steps.length - stageNumber - 1], tmpVar);
		}

		if (this.wfAllStages.get("generateQQManhattanPlots") == 1
				|| this.wfAllStages.get("combGenerateManhattanTop") == 1) {
			for (String elem : manhattans) {
				if (!Arrays.asList(validManhattans).contains(elem)) {
					String errorMessage = CLASS_HEADER + " " + elem
							+ " is not between the supported manhattan plot types: " + validManhattans[0];
					for (int i = 1; i < validManhattans.length; ++i) {
						errorMessage += (" " + validManhattans[i]);
					}
					LOGGER.fatal(errorMessage);
					System.exit(1);
				}
			}
		}
	}

	/**
	 * Method to get the status of a stage. That is if it is active (1) or unactive
	 * (0)
	 * 
	 * @param myStage
	 * @return
	 */
	public int getStageStatus(String myStage) {
		return this.wfAllStages.get(myStage);
	}

	/**
	 * Method to check the existence of a file or a directory defined in the
	 * configuration file
	 * 
	 * @param dirOrFileName
	 */
	public void checkExistence(String dirOrFileName) {
		File theDir = new File(dirOrFileName);
		if (!theDir.exists()) {
			LOGGER.fatal(CLASS_HEADER + " Error, " + dirOrFileName + " does not exist!");
			LOGGER.fatal(CLASS_HEADER + "        Please verify the existence of all your input data set.");
			System.exit(1);
		}
	}

}
