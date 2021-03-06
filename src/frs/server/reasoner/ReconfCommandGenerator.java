/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package frs.server.reasoner;

import frs.server.controller.AnalysisProcedureGenerator;
import frs.server.model.SystemDatabaseHandler;
import java.sql.SQLException;
import java.util.Arrays;
import javax.naming.NamingException;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author abysmli
 */
public class ReconfCommandGenerator {

    private final SystemDatabaseHandler databaseSystem = new SystemDatabaseHandler();
    private final AnalysisProcedureGenerator analysisProcedure;
    private JSONObject mFunctionAnalysis = new JSONObject();

    public ReconfCommandGenerator(AnalysisProcedureGenerator analysisProcedure) {
        this.analysisProcedure = analysisProcedure;
    }

    public JSONObject generate(JSONObject mFunctionAnalysis, JSONArray mTaskList) throws SQLException, NamingException {
        JSONObject resultObj = new JSONObject();
        resultObj.put("command", this.ReconfigurationCommandGenerator(mFunctionAnalysis));
        resultObj.put("redundanz", this.RedundanzAnalyse());
        resultObj.put("restart", this.CheckRestartRequired(mFunctionAnalysis));
        resultObj.put("reconf_function", this.ReconfFunctionGenerator(mFunctionAnalysis));
        resultObj.put("reconf_systemchange", this.ReconfSystemChangeGenerator(mFunctionAnalysis, mTaskList));
        resultObj.put("special_code", this.SpecialCodeGenerator(mFunctionAnalysis));
        resultObj.put("personal_data", this.PersonalData());
        return resultObj;
    }

    private JSONArray ReconfigurationCommandGenerator(JSONObject mFunctionAnalysis) {
        System.out.println();
        this.mFunctionAnalysis = mFunctionAnalysis;
        analysisProcedure.write("Step 1: Generating Reconfiguration Command...");
        JSONArray mMainFunctions = mFunctionAnalysis.getJSONArray("main_functions");
        JSONArray mSubFunctions = mFunctionAnalysis.getJSONArray("sub_functions");
        JSONArray mBasicFunctions = mFunctionAnalysis.getJSONArray("basic_functions");
        String mMainFunctionsCommand = new String("");
        for (int i = 0; i < mMainFunctions.length(); i++) {
            JSONObject obj = mMainFunctions.getJSONObject(i);
            if (obj.getString("availability").equals("true")) {
                mMainFunctionsCommand += "1";
            } else {
                mMainFunctionsCommand += "0";
            }
        }
        String mSubFunctionsCommand = new String("");
        for (int i = 0; i < mSubFunctions.length(); i++) {
            JSONObject obj = mSubFunctions.getJSONObject(i);
            if (obj.getString("availability").equals("true")) {
                mSubFunctionsCommand += "1";
            } else {
                mSubFunctionsCommand += "0";
            }
        }
        String mBasciFunctionsCommand = new String("");
        for (int i = 0; i < mBasicFunctions.length(); i++) {
            JSONObject obj = mBasicFunctions.getJSONObject(i);
            if (obj.getString("availability").equals("true")) {
                mBasciFunctionsCommand += "1";
            } else {
                mBasciFunctionsCommand += "0";
            }
        }
        JSONObject mainFunctionCommand = new JSONObject();
        JSONObject subFunctionCommand = new JSONObject();
        JSONObject basicFunctionCommand = new JSONObject();
        mainFunctionCommand.put("main_function_command", Integer.toString(Integer.parseInt(mMainFunctionsCommand, 2), 16).toUpperCase());
        subFunctionCommand.put("sub_function_command", Integer.toString(Integer.parseInt(mSubFunctionsCommand, 2), 16).toUpperCase());
        basicFunctionCommand.put("basic_function_command", Integer.toString(Integer.parseInt(mBasciFunctionsCommand, 2), 16).toUpperCase());
        JSONArray availableCommand = new JSONArray();
        availableCommand.put(mainFunctionCommand);
        availableCommand.put(subFunctionCommand);
        availableCommand.put(basicFunctionCommand);
        analysisProcedure.write("Reconfiguration Command: ");
        analysisProcedure.write("mainfunction command: " + mMainFunctionsCommand);
        analysisProcedure.write("subfunction Command: " + mSubFunctionsCommand);
        analysisProcedure.write("basicfunction Command: " + mBasciFunctionsCommand);
        System.out.println(availableCommand.toString());
        analysisProcedure.reconfigurationInfo.setReconfigurationsCommand(availableCommand);
        return availableCommand;
    }

    private JSONObject RedundanzAnalyse() {
        System.out.println();
        analysisProcedure.write("Step 2: Redundanz Analysis");
        JSONObject obj = new JSONObject();
        if (mFunctionAnalysis.getBoolean("redundanz")) {
            obj.put("function", "BF5 - BF7");
            obj.put("component", "C3 - C27");
        }
        System.out.println(obj.toString());
        analysisProcedure.reconfigurationInfo.setRedundanzAnalysis(obj);
        return obj;
    }

    private String CheckRestartRequired(JSONObject mFunctionAnalysis) {
        System.out.println();
        analysisProcedure.write("Step 3: Check whether Restart required...");
        analysisProcedure.write("true");
        analysisProcedure.reconfigurationInfo.setRestart("true");
        return "true";
    }

    private JSONObject ReconfFunctionGenerator(JSONObject mFunctionAnalysis) {
        System.out.println();
        analysisProcedure.write("Step 4: Generating Reconfiguration Functions...");
        System.out.println(mFunctionAnalysis.toString());
        analysisProcedure.reconfigurationInfo.setReconfigurationFunctions(mFunctionAnalysis);
        return mFunctionAnalysis;
    }

    private JSONObject ReconfSystemChangeGenerator(JSONObject mFunctionAnalysis, JSONArray mTaskList) throws SQLException, NamingException {
        JSONArray BasicFunctions = mFunctionAnalysis.getJSONArray("basic_functions");
        JSONArray SubFunctions = mFunctionAnalysis.getJSONArray("sub_functions");
        JSONArray MainFunctions = mFunctionAnalysis.getJSONArray("main_functions");

        JSONObject reconfSystemChanged = new JSONObject();
        JSONArray functionFlags = new JSONArray();
        for (int i = 0; i < BasicFunctions.length(); i++) {
            JSONObject basciFunction = BasicFunctions.getJSONObject(i);
            JSONObject obj = new JSONObject();
            if (basciFunction.getString("availability").equals("true")) {
                obj.put("Function_Flag_" + String.valueOf(basciFunction.getInt("function_id")), "true");
            } else {
                obj.put("Function_Flag_" + String.valueOf(basciFunction.getInt("function_id")), "false");
            }
            functionFlags.put(obj);
        }
        reconfSystemChanged.put("functions_flag", functionFlags);
        System.out.println();
        analysisProcedure.write("Step 5: Generating Command for System Change...");
        System.out.println(functionFlags.toString());
        analysisProcedure.reconfigurationInfo.setReconfigurationSystemchange(functionFlags);
        TaskAnalysis(reconfSystemChanged, mTaskList);
        return reconfSystemChanged;
    }

    private JSONObject TaskAnalysis(JSONObject reconfSystemChanged, JSONArray mTaskList) throws SQLException, NamingException {
        System.out.println();
        boolean[] MainfunctionAvailability = new boolean[10];
        Arrays.fill(MainfunctionAvailability, true);
        for (int i = 0; i < mFunctionAnalysis.getJSONArray("main_functions").length(); i++) {
            JSONObject obj = new JSONObject();
            obj = mFunctionAnalysis.getJSONArray("main_functions").getJSONObject(i);
            MainfunctionAvailability[obj.getInt("main_function_id")] = (obj.getString("availability").equals("true"));
        }
        analysisProcedure.write("Step 6: Tasks Analysis...");
        databaseSystem.getTasks();
        JSONArray mTaskAnalysis = new JSONArray();
        for (int i = 0; i < mTaskList.length(); i++) {
            JSONObject taskObj = mTaskList.getJSONObject(i);
            System.out.println();
            analysisProcedure.write("Task Nr.: " + taskObj.getString("task_nr"));
            analysisProcedure.write("Task ID: " + taskObj.getString("task_id"));
            analysisProcedure.write("Task Name: " + taskObj.getString("task_name"));
            // todo: realize the logic

            JSONObject mTaskAnalysisObj = new JSONObject();
            mTaskAnalysisObj.put("task_nr", String.valueOf(i));
            mTaskAnalysisObj.put("task_id", taskObj.getString("task_id"));
            mTaskAnalysisObj.put("task_name", taskObj.getString("task_name"));
            boolean status = true;
            String mFulfilled = "fulfilled";
            String mResult = "normal";
            switch (taskObj.getString("task_id")) {
                case "1":
                    status = MainfunctionAvailability[1] && MainfunctionAvailability[4] && MainfunctionAvailability[3];
                    if (!status) {
                        mFulfilled = "not fulfilled";
                        mResult = "blocked";
                    }
                    mTaskAnalysisObj.put("function_rule", "MF1 = " + MainfunctionAvailability[1] + ", MF3 = " + MainfunctionAvailability[3] + ", MF4 = " + MainfunctionAvailability[4]);
                    mTaskAnalysisObj.put("resource_rule", "Minimum 8L Water in Tank 102");
                    mTaskAnalysisObj.put("resource_rule_result", "fulfilled");
                    mTaskAnalysisObj.put("function_rule_result", mFulfilled);
                    analysisProcedure.write("Task " + taskObj.getString("task_name") + " fulfilled the resource rule: Minimum 8L Water in Tank 102");
                    analysisProcedure.write("Task " + taskObj.getString("task_name") + " " + mFulfilled + " the function rule: MF1 = " + MainfunctionAvailability[1] + ", MF3 = " + MainfunctionAvailability[3] + ", MF4 = " + MainfunctionAvailability[4]);
                    mTaskList.getJSONObject(i).put("status", mResult);
                    break;
                case "2":
                    status = MainfunctionAvailability[3];
                    if (!status) {
                        mFulfilled = "not fulfilled";
                        mResult = "blocked";
                    }
                    mTaskAnalysisObj.put("resource_rule", "Minimum 5L Water in Tank 102");
                    mTaskAnalysisObj.put("resource_rule_result", "fulfilled");
                    mTaskAnalysisObj.put("function_rule", "MF3 = " + MainfunctionAvailability[3]);
                    mTaskAnalysisObj.put("function_rule_result", mFulfilled);
                    analysisProcedure.write("Task " + taskObj.getString("task_name") + " fulfilled the resource rule: Minimum 5L Water in Tank 102");
                    analysisProcedure.write("Task " + taskObj.getString("task_name") + " " + mFulfilled + " the function rule: MF3 = " + MainfunctionAvailability[3]);
                    mTaskList.getJSONObject(i).put("status", mResult);
                    break;
                case "3":
                    status = MainfunctionAvailability[2];
                    if (!status) {
                        mFulfilled = "not fulfilled";
                        mResult = "blocked";
                    }
                    mTaskAnalysisObj.put("resource_rule", "Minimum 3L Water in Tank 102");
                    mTaskAnalysisObj.put("resource_rule_result", "fulfilled");
                    mTaskAnalysisObj.put("function_rule", "MF2 = " + MainfunctionAvailability[2]);
                    mTaskAnalysisObj.put("function_rule_result", mFulfilled);
                    analysisProcedure.write("Task " + taskObj.getString("task_name") + " fulfilled the resource rule: Minimum 3L Water in Tank 102");
                    analysisProcedure.write("Task " + taskObj.getString("task_name") + " " + mFulfilled + " the function rule: MF2 = " + MainfunctionAvailability[2]);
                    mTaskList.getJSONObject(i).put("status", mResult);
                    break;
                case "4":
                    status = MainfunctionAvailability[1] && MainfunctionAvailability[3];
                    if (!status) {
                        mFulfilled = "not fulfilled";
                        mResult = "blocked";
                    }
                    mTaskAnalysisObj.put("resource_rule", "Minimum 8L Water in Tank 102");
                    mTaskAnalysisObj.put("resource_rule_result", "fulfilled");
                    mTaskAnalysisObj.put("function_rule", "MF1 = " + MainfunctionAvailability[1] + ", MF3 = " + MainfunctionAvailability[3]);
                    mTaskAnalysisObj.put("function_rule_result", mFulfilled);
                    analysisProcedure.write("Task " + taskObj.getString("task_name") + " fulfilled the resource rule: Minimum 8L Water in Tank 102");
                    analysisProcedure.write("Task " + taskObj.getString("task_name") + " " + mFulfilled + " the function rule: MF1 = " + MainfunctionAvailability[1] + ", MF3 = " + MainfunctionAvailability[3]);
                    mTaskList.getJSONObject(i).put("status", mResult);
                    break;
                default:
                    break;
            }
            mTaskAnalysis.put(mTaskAnalysisObj);
        }
        System.out.println();
        analysisProcedure.write("Task List after analysis: ");
        System.out.println(mTaskList.toString());
        analysisProcedure.reconfigurationInfo.setTaskAnalysis(mTaskAnalysis);
        analysisProcedure.reconfigurationInfo.setTaskList(mTaskList);
        reconfSystemChanged.put("task_list", mTaskList);
        return reconfSystemChanged;
    }

    private String SpecialCodeGenerator(JSONObject mFunctionAnalysis) {
        System.out.println();
        analysisProcedure.write("Step 7: Generating Special Code...");
        String mSpecialCode = "";
        if (mFunctionAnalysis.getBoolean("redundanz")) {
            mSpecialCode = "temp = temperaturDisplay2.getTemperatur()";
        }
        analysisProcedure.write("Special Code: " + mSpecialCode);
        analysisProcedure.reconfigurationInfo.setSpecialCode(mSpecialCode);
        return mSpecialCode;
    }

    private JSONObject PersonalData() {
        System.out.println();
        analysisProcedure.write("Step 8: Get Personal Data");
        JSONObject obj = new JSONObject();
        obj.put("General_Techniker", "Wang, Huiqiang +49 123 4567 899");
        obj.put("Wartungsdienst", "Hui, Wangqiang +49 321 2233 899");
        obj.put("Expert", "Qiang, Wangqiang +49 333 4567 888");
        System.out.println(obj.toString());
        analysisProcedure.reconfigurationInfo.setPersonalData(obj);
        return obj;
    }
}
