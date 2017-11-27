package com.thinkhr.external.api.services.utils;

import static com.thinkhr.external.api.ApplicationConstants.COMMA_SEPARATOR;
import static com.thinkhr.external.api.ApplicationConstants.FAILED_COLUMN_TO_IMPORT;
import static com.thinkhr.external.api.ApplicationConstants.FILE_IMPORT_RESULT_MSG;
import static com.thinkhr.external.api.ApplicationConstants.REQUIRED_HEADERS_COMPANY_CSV_IMPORT;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.thinkhr.external.api.exception.APIErrorCodes;
import com.thinkhr.external.api.exception.ApplicationException;
import com.thinkhr.external.api.exception.MessageResourceHandler;
import com.thinkhr.external.api.model.FileImportResult;
import com.thinkhr.external.api.request.APIRequestHelper;
import com.thinkhr.external.api.response.APIMessageUtil;

/**
 * A utility class for performing all operations
 * on File Import.
 *  
 * @since 2-17-11-17
 * @author Ajay
 *
 */
public class FileImportUtil {
	
    /**
     * Finds if any required header is missing from given set of headers
     * 
     * @param String[] headersInFile
     * @param String[] requiredHeaders
     * @return String[] Array of missing headers 
     */
    public static String[] getMissingHeaders(String[] presentHeaders, String[] requiredHeaders) {
        Set<String> headersInFileSet = new HashSet<String>(Arrays.asList(presentHeaders));
        Set<String> requiredHeadersSet = new HashSet<String>(Arrays.asList(requiredHeaders));
        requiredHeadersSet.removeAll(headersInFileSet);

        String[] missingHeaders = new String[requiredHeadersSet.size()];
        return requiredHeadersSet.toArray(missingHeaders);
    }

  
    /**
     * Reads file content for given CSV
     * 
     * @param file
     * @return
     * @throws IOException
     */
    public static List<String> readFileContent(MultipartFile file) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(file.getInputStream()));
            return br.lines().collect(Collectors.toList());
        } catch (IOException ioe) {
            throw ApplicationException.createBadRequest(APIErrorCodes.FILE_READ_ERROR, file.getName());
        }
    }
  
    /**
     * This method populates the given array columnValues from 
     * columns,columnToHeaderMap,splitValues,headerIndexMap
     * 
     * columns is array of columns in database
     * columnToHeaderMap is a map containing key as column name and value as its mapping column name in csv
     * splitValues is array of values created by splitting comma seperated csv record
     * headerIndexMap is a map containing column name in csv as key and value is the index at which this column is found in imported csv. Starting index is 1
     * 
     * Currently this function assumes size for columnValues array and columns array is same. 
     * Difference in sizes will have unpredictable results.
     * 
     */
    public static Object[] populateColumnValues(String fileRow, Map<String, String> columnToHeaderMap,
         Map<String, Integer> headerIndexMap) {
        
        String[] rowColValues = fileRow.split(COMMA_SEPARATOR);
        
        Object[] companyColumnsValues = new Object[columnToHeaderMap.size()];
        
        int k = 0;
       
        for (String column: rowColValues) {
            
            String headerinCsv = columnToHeaderMap.get(column); // get the expected csv header corresponding to column

            //get index at which value corresponding to this column is found in csv
            Integer headerIndex = headerIndexMap.get(headerinCsv);

            if (headerinCsv == null && headerIndex == null) { // CSV contains the mapped header
                // SKIP and continue;
                continue;
            }

            companyColumnsValues[k++] = rowColValues[headerIndex]; //This line throwing ArrayIndexOutOfBound exception means split record does nt have the value for this column
        }
        
        return companyColumnsValues;
    }

    /**
     * This Function will create a response csv file from FileImportResult
     * @param FileimportResult fileImportResult
     * @return File
     * @throws IOException
     */
    public static File createReponseFile(FileImportResult fileImportResult, MessageResourceHandler resourceHandler) throws IOException {
        File responseFile = File.createTempFile("fileImportResponse", ".csv");
        FileWriter writer = new FileWriter(responseFile);

        if (fileImportResult != null) {
            String msg = APIMessageUtil.getMessageFromResourceBundle(resourceHandler, FILE_IMPORT_RESULT_MSG,
                    String.valueOf(fileImportResult.getTotalRecords()), String.valueOf(fileImportResult.getNumSuccessRecords()),
                    String.valueOf(fileImportResult.getNumFailedRecords()));

            String jobId = (String) APIRequestHelper.getRequestAttribute("jobId");
            writer.write("Job Id :" + jobId + "\n");
            writer.write(msg);
            if (fileImportResult.getNumFailedRecords() > 0) {
                String title = APIMessageUtil.getMessageFromResourceBundle(resourceHandler, "FAILED_RECORDS");
                writer.write(title + "\n");
                String columnForFailureCause = APIMessageUtil.getMessageFromResourceBundle(resourceHandler, "FAILURE_CAUSE");
                writer.write(fileImportResult.getHeaderLine() + "," + columnForFailureCause + "\n");
                for (FileImportResult.FailedRecord failedRecord : fileImportResult.getFailedRecords()) {
                    writer.write(failedRecord.getRecord() + "," + failedRecord.getFailureCause() + "\n");
                }
            }
        }
        writer.close();
        return responseFile;
    }

    
    /**
     * Check if all customHeaders in csv has a database field  to which its value should be mapped.
     * If any custom header does not have mapping field then throw exception.
     * 
     * Not able to use batch processing here due to requirement of
     * dealing with failure records & adding data in more than one tables.
     * 
     * @param allHeadersInCsv
     * @param allMappedHeaders
     */
    public static void validateAndFilterCustomHeaders(String[] allHeadersInCsv, 
    									  Collection<String> allMappedHeaders) {
        
        Set<String> requiredHeadersSet = new HashSet<String>(Arrays.asList(REQUIRED_HEADERS_COMPANY_CSV_IMPORT));
        
        // Filter failedCausedColumn and already mapped columns from custom header list.  
        String[] customHeaders = (String[]) Arrays.stream(allHeadersInCsv).filter(x -> !requiredHeadersSet.contains(x) && 
                                                                                       !allMappedHeaders.contains(x) && 
                                                                                       !FAILED_COLUMN_TO_IMPORT.equalsIgnoreCase(x)).toArray();
        if (customHeaders == null || customHeaders.length <= 0) {
            throw ApplicationException.createFileImportError(APIErrorCodes.UNMAPPED_CUSTOM_HEADERS, StringUtils.join(customHeaders, COMMA_SEPARATOR));
        }
    }


}