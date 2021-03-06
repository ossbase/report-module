package ru.osslabs.modules.report.spu.domain;

import lombok.Data;
import ru.osslabs.modules.report.CMDBuildField;
import ru.osslabs.modules.report.domain.*;

import java.util.List;

/**
 * Created by Serge Kozyrev on 11.12.15.
 */
@Data
public class Service {
    private Integer id;
    @CMDBuildField(name = "fullname")
    private String fullName;

    @CMDBuildField(name = "Description")
    private String description;

    @CMDBuildField(name = "Desc2")
    private String orgGovernmentDesc;

    @CMDBuildField(name = "FederalNumberOfService")
    private String federalNumberOfService;

    @CMDBuildField(name = "nameservice")
    private String nameService;

    @CMDBuildField(name = "Servicecommunication")
    private List<SubServices> subServices;

    @CMDBuildField(name = "ARegl")
    private List<Normative> aRegl;

    private CMDField<Boolean> radiotelephone;
    private CMDField<Boolean> terminal;
    private CMDField<Boolean> officialSite;
    private String websiteAddress;
    private CMDField<Boolean> portalPublicServices;
    private CMDField<Boolean> siteVashControl;
    @CMDBuildField(name = "RefQualityRating")
    private List<RefQualityRating> refQualityRating;
    @CMDBuildField(name = "RefOrgGovemment")
    private List<RefOrgGovemment> refOrgGovemment;
    @CMDBuildField(name = "NalichieApprovedTKMW")
    private List<DescriptionTKMW> nalichieApprovedTKMW;
    @CMDBuildField(name = "objAppeal3")
    private SubServices objAppeal;
    private Lookup<String> havesubservices;

/*
    HaveQualityRating,
    typicalAgreement,
    AReglLookUp,
    tkmvlLookUp,
    otherMethods,
    haveTypicalAgreement,
*/
}
