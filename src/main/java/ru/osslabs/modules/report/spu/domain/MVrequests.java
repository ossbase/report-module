package ru.osslabs.modules.report.spu.domain;

import lombok.Data;
import ru.osslabs.modules.report.CMDBuildField;
import ru.osslabs.modules.report.domain.*;


/**
 * Created by Serge Kozyrev on 12.01.16.
 */
@Data
public class MVrequests {
    private Integer id;
    @CMDBuildField(name = "InformationContent")
    private String informationContent;
    @CMDBuildField(name = "NamOrgGuiInteRequest2")
    private OrgGovernment namOrgGuiInteRequest;
    @CMDBuildField(name = "NamAuthSeInteReq3")
    private OrgGovernment mamAuthSeInteReq;
    @CMDBuildField(name = "SIDElectService1")
    private String sidElectService;
    @CMDBuildField(name = "RequestExecutionTerm")
    private Integer requestExecutionTerm;
    private Lookup<String> unitAllrequest;
    private Integer requestingMV;
    private Lookup<String> unitrequestingMV;
    private Integer directionResponse;
    private Lookup<String> unitdirectionResponse;
    private Integer communionResponse;
    @CMDBuildField(name = "ComResponse")
    private Lookup<String> comResponse;
/*  //FILE
    private Object replyFormMV;
    //FILE
    private Object patternResponseMV;
*/
    //FILE
    @CMDBuildField(name = "FileFormInReqText")
    private FileList fileFormInReqText;
    //FILE
    @CMDBuildField(name = "FileFillPatText")
    private FileList fileFillPatText;

}
