package ru.osslabs.modules.report.transformers;

import javaslang.Tuple2;
import javaslang.control.*;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.CellReference;
import ru.osslabs.modules.report.Matrix;
import ru.osslabs.modules.report.decorators.SourceFututeHSSFWorkBookReport;
import ru.osslabs.modules.report.domain.*;
import ru.osslabs.modules.report.isui.domain.*;
import ru.osslabs.modules.report.prc.domain.Direction;
import ru.osslabs.modules.report.spu.domain.*;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static javaslang.collection.Stream.ofAll;
import static ru.osslabs.modules.report.ReportUtils.objectNotNull;

/**
 * Created by ikuchmin on 03.11.15.
 */
public class HSSFWorkbookTransformers {
    private static Locale RUSSIAN = new Locale("ru", "RU");
    private static final String NO = "Нет";
    private static final String SPACE = " ";
    private static final String EMPTY = "";
    private static final String HYPHEN = "-";
    private static final String NO_DATA = "Данные не заполнены";
    private static final String FILE_IS_ATTACHED = "Файл приложен";
    private static final String FILE_IS_NOT_ATTACHED = "Файл не приложен";

    public static <Re extends SourceFututeHSSFWorkBookReport> HSSFWorkbook fromMatrixToHSSFWorkbook(Re report, Matrix<Double> data) {
        // TODO: Возможно стоит как-то ограничить время на Future<V>::get
        HSSFWorkbook workbook = report.getHSSFWorkbookFuture().get();
        CellReference ref = new CellReference(workbook.getName(report.getDataBagCellName()).getRefersToFormula());
        HSSFSheet sheet = workbook.getSheet(ref.getSheetName());
        data.traversal((row, col, el) -> {
            int actualRow = row + ref.getRow();
            int actualCol = col + ref.getCol();
            HSSFRow rowCells = objectNotNull(actualRow, sheet::getRow, sheet::createRow);
            HSSFCell cell = objectNotNull(actualCol, rowCells::getCell, rowCells::createCell);
            cell.setCellValue(el);
        });
        return workbook;
    }

    public static <Re extends SourceFututeHSSFWorkBookReport> HSSFWorkbook fromListMatrixToHSSFWorkbook(Re report, List<List<Double>> data) {
        return fromMatrixToHSSFWorkbook(report, Matrix.of(data));
    }

    /**
     * First uses is report for SED
     *
     * @param report
     * @param data
     * @param <Re>
     * @return
     */
    public static <Re extends SourceFututeHSSFWorkBookReport> HSSFWorkbook fromStreamTuplesToHSSFWorkbook(Re report, Stream<Tuple2<String, Integer>> data) {
        // TODO: Возможно стоит как-то ограничить время на Future<V>::get
        HSSFWorkbook workbook = report.getHSSFWorkbookFuture().get();
        CellReference ref = new CellReference(workbook.getName(report.getDataBagCellName()).getRefersToFormula());
        HSSFSheet sheet = workbook.getSheet(ref.getSheetName());

        AtomicInteger rowIdx = new AtomicInteger(0);
        data.forEach((tupl) -> {
            int actualRow = rowIdx.getAndIncrement() + ref.getRow();
            HSSFRow rowCells = objectNotNull(actualRow, sheet::getRow, sheet::createRow);

            HSSFCell cell_1 = objectNotNull((int) ref.getCol(), rowCells::getCell, rowCells::createCell);
            cell_1.setCellValue(tupl._1);

            HSSFCell cell_2 = objectNotNull(ref.getCol() + 1, rowCells::getCell, rowCells::createCell);
            cell_2.setCellValue(tupl._2);
        });

        return workbook;
    }

    public static <Re extends SourceFututeHSSFWorkBookReport> HSSFWorkbook fromStreamServiceToFirstReport(Re report, Option<Service> serviceOption) {
        HSSFWorkbook workbook = report.getHSSFWorkbookFuture().get();
        CellReference ref = new CellReference(workbook.getName(report.getDataBagCellName()).getRefersToFormula());
        HSSFSheet sheet = workbook.getSheet(ref.getSheetName());
        AtomicInteger rowIdx = new AtomicInteger(0);
        if (serviceOption.isDefined()) {
            Service service = serviceOption.get();
            //1
            Row.of(objectNotNull(rowIdx.getAndIncrement() + ref.getRow(), sheet::getRow, sheet::createRow), ref.getCol())
                .addCellWithValue(String.format("%d", rowIdx.get()))
                .addCellWithValue("Наименование органа, предоставляющего услугу")
                .addCellWithValue(String.format("%s",
                    ofAll(service.getRefOrgGovemment())
                        .headOption()
                        .map(RefOrgGovemment::getFullname)
                        .orElse(NO)))
                .addCellWithValue(ZonedDateTime.now(ZoneId.of("Europe/Moscow")).format(DateTimeFormatter.ofPattern("dd.MM.uuuu", RUSSIAN)));
            //2
            Row.of(objectNotNull(rowIdx.getAndIncrement() + ref.getRow(), sheet::getRow, sheet::createRow), ref.getCol())
                .addCellWithValue(String.format("%d", rowIdx.get()))
                .addCellWithValue("Номер услуги в федеральном реестре")
                .addCellWithValue(Option.of(service.getFederalNumberOfService()).orElse(NO));
            //3
            Row.of(objectNotNull(rowIdx.getAndIncrement() + ref.getRow(), sheet::getRow, sheet::createRow), ref.getCol())
                .addCellWithValue(String.format("%d", rowIdx.get()))
                .addCellWithValue("Полное наименование услуги")
                .addCellWithValue(Option.of(service.getDescription()).orElse(NO));
            //4
            Row.of(objectNotNull(rowIdx.getAndIncrement() + ref.getRow(), sheet::getRow, sheet::createRow), ref.getCol())
                .addCellWithValue(String.format("%d", rowIdx.get()))
                .addCellWithValue("Краткое наименование услуги")
                .addCellWithValue(Option.of(service.getNameService()).orElse(NO));
            //5
            Row.of(objectNotNull(rowIdx.getAndIncrement() + ref.getRow(), sheet::getRow, sheet::createRow), ref.getCol())
                .addCellWithValue(String.format("%d", rowIdx.get()))
                .addCellWithValue("Административный регламент предоставления государственной услуги")
                .addCellWithValue(ofAll(service.getARegl())
                    .headOption()
                    .map(npa -> String.format(RUSSIAN, "%1$s\n" +
                            "орган власти, утвердивший административный регламент: %5$s.\n" +
                            "от %2$td.%2$tm.%2$tY № %3$s\n" +
                            "%4$s",
                        Option.of(npa.getTYPE_NPA())
                            .map(NormativeType::getDescription)
                            .orElse(SPACE),
                        npa.getDateNPA(),
                        npa.getNumberNPA(),
                        npa.getNameNPA(),
                        Option.of(npa.getOgv_NPA())
                            .map(OgvGovernment::getFullName)
                            .orElse(SPACE)))
                    .filter(v -> !(v.trim().isEmpty()))
                    .orElse(NO));
            //6
            Row.of(objectNotNull(rowIdx.getAndIncrement() + ref.getRow(), sheet::getRow, sheet::createRow), ref.getCol())
                .addCellWithValue(String.format("%d", rowIdx.get()))
                .addCellWithValue("Перечень «подуслуг»")
                .addCellWithValue(() -> {
                    Lookup<String> haveSubServices = service.getHavesubservices();
                    if (haveSubServices == null) {
                        return NO;
                    }
                    if (haveSubServices.getValue().equals("Да")) {
                        AtomicInteger index = new AtomicInteger();
                        return ofAll(service.getSubServices())
                            .filter(Objects::nonNull)
                            .map(SubServices::getDescription)
                            .map(v -> String.format("%d. %s", index.incrementAndGet(), v))
                            .reduceLeftOption(HSSFWorkbookTransformers::joiningNewLine)
                            .orElse(NO);
                    } else {
                        return NO;
                    }
                });
            //7
            Row.of(objectNotNull(rowIdx.getAndIncrement() + ref.getRow(), sheet::getRow, sheet::createRow), ref.getCol())
                .addCellWithValue(String.format("%d", rowIdx.get()))
                .addCellWithValue("Способы оценки качества предоставления государственной услуги")
                .addCellWithValue(() -> {
                    AtomicInteger index = new AtomicInteger();
                    return ofAll(
                        ofAll(service.getRadiotelephone(),
                            service.getTerminal(),
                            service.getPortalPublicServices(),
                            service.getSiteVashControl())
                            .filter(cm -> cm.getValue().filter(v -> v.equals(true)).isDefined()) // Replace true on false
                            .map(CMDField::getDescription),
                        Option.of(service.getOfficialSite())
                            .filter(cm -> cm.getValue().filter(v -> v.equals(true)).isDefined())
                            .map(cm -> String.format(RUSSIAN, "%s %s", cm.getDescription(), service.getWebsiteAddress())),
                        ofAll(service.getRefQualityRating()).map(RefQualityRating::getDescription)
                            .reduceLeftOption(HSSFWorkbookTransformers::joiningNewLine))
                        .flatMap(Function.identity())
                        .map(v -> String.format("%d. %s", index.incrementAndGet(), v))
                        .reduceLeftOption(HSSFWorkbookTransformers::joiningNewLine).orElse(NO);
                });
        }
        return workbook;
    }

    public static <Re extends SourceFututeHSSFWorkBookReport> HSSFWorkbook fromStreamServiceToSecondReport(Re report, Option<Service> serviceOption) {
        HSSFWorkbook workbook = report.getHSSFWorkbookFuture().get();
        CellReference ref = new CellReference(workbook.getName(report.getDataBagCellName()).getRefersToFormula());
        HSSFSheet sheet = workbook.getSheet(ref.getSheetName());

        AtomicInteger rowIdx = new AtomicInteger(0);
        if (serviceOption.isDefined()) {
            Service service = serviceOption.get();
            ofAll(service.getSubServices())
                .append(service.getObjAppeal())
                .filter(Objects::nonNull)
                .forEach((ss) -> {
                    Row.of(objectNotNull(rowIdx.getAndIncrement() + ref.getRow(), sheet::getRow, sheet::createRow), ref.getCol())
                        //1
                        .addCellWithValue(String.format("%d", rowIdx.get())) // If you confuse, which number was row. Here we see rowIndx + 1 because in Row.of we saw rowIdx.getAndIncrement()
                        //2
                        .addCellWithValue(Option.of(ss.getDescription())
                            .orElse(NO))
                        //3
                        .addCellWithValue(Option.of(ss.getPeriodsubservice())
                            .map((val) -> String.format(RUSSIAN, "%d %s (%s)", val, ss.getFormPeriodSubservice().getValue(), ss.getComm()))
                            .orElse(NO))
                        //4
                        .addCellWithValue(Option.of(ss.getPeriodSubservice_ExTerr())
                            .map((val) -> String.format(RUSSIAN, "%d %s (%s)", val, ss.getFormPeriodSubservice_ExTer().getValue(), ss.getCommen()))
                            .orElse(NO))
                        //5
                        .addCellWithValue(ofAll(ss.getReject_noRecept())
                            .map(Rejection::getDescription)
                            .map("- "::concat)
                            .reduceLeftOption(HSSFWorkbookTransformers::joiningNewLine)
                            .orElse(NO))
                        //6
                        .addCellWithValue(ofAll(ss.getRejection_noProv())
                            .map(Rejection::getDescription)
                            .map("- "::concat)
                            .reduceLeftOption(HSSFWorkbookTransformers::joiningNewLine)
                            .orElse(NO))
                        //7
                        .addCellWithValue(ofAll(ss.getRejection_noAct())
                            .map(Rejection::getDescription)
                            .map("- "::concat)
                            .reduceLeftOption(HSSFWorkbookTransformers::joiningNewLine)
                            .orElse(NO))
                        //8
                        .addCellWithValue(() -> {
                            String period = null;
                            String units = null;

                            Option<Rejection> rejectionOption = ofAll(ss.getRejection_noAct()).headOption();
                            if (rejectionOption.isDefined()) {
                                Rejection rejection = rejectionOption.get();
                                period = rejection.getPeriodSuspension();
                                units = Option.of(rejection.getUnitTerms()).map(Lookup::getValue).orElse(null);
                            }


                            if (period == null && units == null) {
                                period = Option.of(ss.getSuspension_days())
                                    .map(String::valueOf)
                                    .orElse(EMPTY);
                                units = Option.of(ss.getFormSuspension_days())
                                    .map(Lookup::getValue)
                                    .orElse(EMPTY);
                            }
                            String cellText = ofAll(period, units)
                                .filter(v -> !v.isEmpty())
                                .reduceLeftOption(HSSFWorkbookTransformers::joiningWithSpace)
                                .orElse(HYPHEN);

                            return cellText;
                        })
                        //9
                        .addCellWithValue(ofAll(ss.getSubservice_Payment2())
                            .map((p) ->
                                String.format(RUSSIAN, "%s. Размер государственной пошлины или иной платы: %d руб. (%s)",
                                    p.getDescription(),
                                    p.getSizepayment(),
                                    p.getMessage())
                            )
                            .map("- "::concat)
                            .reduceLeftOption(HSSFWorkbookTransformers::joiningNewLine)
                            .orElse(NO))
                        //10
                        .addCellWithValue(ofAll(ss.getSubservice_Payment2()) // 7.2
                            .flatMap(p -> ofAll(p.getPayment_npa())
                                .map((npa) -> String.format(RUSSIAN,
                                    "%1$s орган власти, утвердивший административный регламент: %2$s от %3$td.%3$tm.%3$tY № %4$s\n%5$s.\nПункт: %6$s",
                                    Option.of(npa.getTYPE_NPA())
                                        .map(NormativeType::getDescription)
                                        .orElse(SPACE),
                                    Option.of(npa.getOgv_NPA())
                                        .map(OgvGovernment::getFullName)
                                        .orElse(SPACE),
                                    npa.getDateNPA(),
                                    npa.getNumberNPA(),
                                    npa.getNameNPA(),
                                    p.getPointForPayment())))
                            .toSet()
                            .map("- "::concat)
                            .reduceLeftOption(HSSFWorkbookTransformers::joiningNewLine)
                            .orElse(HYPHEN))
                        //11
                        .addCellWithValue(ofAll(ss.getSubservice_Payment2()) // 7.3
                            .map(pa -> String.format(RUSSIAN,
                                "КБК при обращении в орган власти: %s. КБК при обращении в МФЦ: %s",
                                pa.getKbk_OGV(), pa.getKbk_MFC()))
                            .toSet()
                            .map("- "::concat)
                            .reduceLeftOption((acc, ps) -> acc.concat("\n").concat(ps))
                            .orElse(HYPHEN))
                        //12
                        .addCellWithValue(ofAll(
                            ofAll(ss.getLichnoVOrgan(),
                                ss.getLichnoVTerrOrgan(),
                                ss.getLichnoVMFC(),
                                ss.getPortalGosUslig(),
                                ss.getPost())
                                .filter(cm -> cm.getValue().filter(v -> v.equals(true)).isDefined())
                                .map(CMDField::getDescription),
                            Option.of(ss.getOffSiteOrganaUslugi())
                                .filter(cm -> cm.getValue().filter(v -> v.equals(true)).isDefined())
                                .map(cm -> String.format(RUSSIAN, "%s %s", cm.getDescription(), ss.getAdressOffSite())),
                            ofAll(ss.getAppealSubServices()).map(SebserviceAppeal::getDescription)
                                .reduceLeftOption(HSSFWorkbookTransformers::joiningNewLine))
                            .flatMap(Function.identity())
                            .map("- "::concat)
                            .reduceLeftOption(HSSFWorkbookTransformers::joiningNewLine)
                            .orElse(NO))
                        //13
                        .addCellWithValue(ofAll(
                            ofAll(ss.getAuthorityPaper(),
                                ss.getTerrOrgOnPaper(),
                                ss.getInMFConPaperFrom(),
                                ss.getInMFCinDocFromITOrg(),
                                ss.getFromCabinetGosUslug(),
                                ss.getFromGosUslugInELForm(),
                                ss.getEmailDocWithElSignature(),
                                ss.getPostResult())
                                .filter(cm -> cm.getValue().filter(v -> v.equals(true)).isDefined()) // Replace true on false
                                .map(CMDField::getDescription),
                            Option.of(ss.getFromCabinetOffSite())
                                .filter(cm -> cm.getValue().filter(v -> v.equals(true)).isDefined())
                                .map(cm -> String.format(RUSSIAN, "%s %s", cm.getDescription(), ss.getAddresOffSiteResult())),
                            Option.of(ss.getFromOffSiteElDoc())
                                .filter(cm -> cm.getValue().filter(v -> v.equals(true)).isDefined())
                                .map(cm -> String.format(RUSSIAN, "%s %s", cm.getDescription(), ss.getAddresOffSiteELDoc())),
                            ofAll(ss.getAppealSubServices()).map(SebserviceAppeal::getDescription)
                                .reduceLeftOption(HSSFWorkbookTransformers::joiningNewLine))
                            .flatMap(Function.identity())
                            .map("- "::concat)
                            .reduceLeftOption(HSSFWorkbookTransformers::joiningNewLine).orElse(NO))
                        //14
                        .addCellWithValue(ZonedDateTime.now(ZoneId.of("Europe/Moscow")).format(DateTimeFormatter.ofPattern("dd.MM.uuuu", RUSSIAN)))
                        //15
                        .addCellWithValue(service.getDescription());
                });
        }
        return workbook;
    }

    public static <Re extends SourceFututeHSSFWorkBookReport> HSSFWorkbook fromStreamServiceToThirdReport(Re report, Option<Service> serviceOption) {
        HSSFWorkbook workbook = report.getHSSFWorkbookFuture().get();
        CellReference ref = new CellReference(workbook.getName(report.getDataBagCellName()).getRefersToFormula());
        HSSFSheet sheet = workbook.getSheet(ref.getSheetName());
        AtomicInteger rowIdx = new AtomicInteger(0);

        if (serviceOption.isDefined()) {
            Service service = serviceOption.get();
            List<SubServices> subServices = ofAll(service.getSubServices())
                .append(service.getObjAppeal())
                .filter(Objects::nonNull)
                .toJavaList();
            for (SubServices subService : subServices) {
                //перечень заявителей подуслуги
                List<ApplicantsCircleSubservice> applicantList = subService.getFillDetailsApplicant();
                if (applicantList.isEmpty()) {
                    //выводим пустую строку
                    Row.of(objectNotNull(rowIdx.getAndIncrement() + ref.getRow(), sheet::getRow, sheet::createRow), ref.getCol())
                        //1
                        .addCellWithValue(String.format("%d", rowIdx.get()))
                        //2
                        .addCellWithValue(SPACE)
                        //3
                        .addCellWithValue(SPACE)
                        //4
                        .addCellWithValue(SPACE)
                        //5
                        .addCellWithValue(SPACE)
                        //6
                        .addCellWithValue(SPACE)
                        //7
                        .addCellWithValue(SPACE)
                        //8
                        .addCellWithValue(SPACE)
                        //9
                        .addCellWithValue(ZonedDateTime.now(ZoneId.of("Europe/Moscow")).format(DateTimeFormatter.ofPattern("dd.MM.uuuu", RUSSIAN)))
                        //10
                        .addCellWithValue(Option.of(service.getDescription()).orElse(NO))
                        //11
                        .addCellWithValue(Option.of(subService.getDescription()).orElse(NO))
                        //12
                        .addCellWithValue(NO_DATA)
                        //13
                        .addCellWithValue(SPACE)
                        //14
                        .addCellWithValue(SPACE)
                        //15
                        .addCellWithValue(SPACE)
                        //16
                        .addCellWithValue(SPACE);
                } else {
                    //перечень документов подуслуги
                    List<DescriptionDocumentsObslugi> docList = subService.getFillDocSubservice();
                    //мапим заявителя на список его документов
                    Map<ApplicantsCircleSubservice, List<DescriptionDocumentsObslugi>> applicantToDocMap = new HashMap<>();
                    for (ApplicantsCircleSubservice applicant : applicantList) {
                        List<DescriptionDocumentsObslugi> list = new ArrayList<>();
                        for (DescriptionDocumentsObslugi doc : docList) {
                            for (ApplicantsCircleSubservice applicantFromDoc : doc.getCategPersoSubservices()) {
                                if (applicant.equals(applicantFromDoc)) {
                                    list.add(doc);
                                }
                            }
                        }
                        applicantToDocMap.put(applicant, list);
                    }
                    //мапим заявителя на список документов представителя заявителя
                    Map<ApplicantsCircleSubservice, List<DescriptionDocumentsObslugi>> applicantToRepresDocMap = new HashMap<>();
                    for (ApplicantsCircleSubservice applicant : applicantList) {
                        List<DescriptionDocumentsObslugi> list = new ArrayList<>();
                        for (DescriptionDocumentsObslugi doc : docList) {
                            for (DescribeRepresentativesApplicants representative : doc.getApplicRepresentative()) {
                                if (applicant.getPerStatemBehApplicant().contains(representative)) {
                                    list.add(doc);
                                }
                            }
                        }
                        applicantToRepresDocMap.put(applicant, list);
                    }

                    for (ApplicantsCircleSubservice applicantDesc : applicantList) {
                        List<DescriptionDocumentsObslugi> applicantDocList = applicantToDocMap.get(applicantDesc);
                        List<DescriptionDocumentsObslugi> represDocList = applicantToRepresDocMap.get(applicantDesc);
                        if (applicantDocList.isEmpty() && represDocList.isEmpty()) {
                            //оба списка пусты - выводим пустые ячейки
                            Row.of(objectNotNull(rowIdx.getAndIncrement() + ref.getRow(), sheet::getRow, sheet::createRow), ref.getCol())
                                //1
                                .addCellWithValue(String.format("%d", rowIdx.get()))
                                //2
                                .addCellWithValue(SPACE)
                                //3
                                .addCellWithValue(NO)
                                //4
                                .addCellWithValue(NO)
                                //5
                                .addCellWithValue(SPACE)
                                //6
                                .addCellWithValue(SPACE)
                                //7
                                .addCellWithValue(HYPHEN)
                                //8
                                .addCellWithValue(HYPHEN)
                                //9
                                .addCellWithValue(ZonedDateTime.now(ZoneId.of("Europe/Moscow")).format(DateTimeFormatter.ofPattern("dd.MM.uuuu", RUSSIAN)))
                                //10
                                .addCellWithValue(Option.of(service.getDescription()).orElse(NO))
                                //11
                                .addCellWithValue(Option.of(subService.getDescription()).orElse(NO))
                                //12
                                .addCellWithValue(
                                    ofAll(applicantDesc.getApplicantsCategorySubservice())
                                        .headOption()
                                        .map(PersonsEntitledReceivePodology::getDescription)
                                        .orElse(NO))
                                //13
                                .addCellWithValue(SPACE)
                                //14
                                .addCellWithValue(
                                    Option.of(applicantDesc.getPossibilityApplication())
                                        .filter(v->v.getValue() != null)
                                        .map(v -> {
                                            if (v.getValue().toLowerCase().equals("имеется")) {
                                                return "Имеется";
                                            } else {
                                                return HYPHEN;
                                            }
                                        })
                                        .orElse(HYPHEN)
                                )
                                //15
                                .addCellWithValue(
                                    ofAll(applicantDesc.getPerStatemBehApplicant())
                                        .flatMap(DescribeRepresentativesApplicants::getCatPerEntiRecObslu)
                                        .map(Representatives::getDescription)
                                        .reduceLeftOption(HSSFWorkbookTransformers::joiningNewLine)
                                        .orElse(HYPHEN)
                                )
                                //16
                                .addCellWithValue(SPACE);
                        } else {
                            //главный список, по которому будем выводить строки
                            //он должен быть длиннее, чем второстепенный
                            List<DescriptionDocumentsObslugi> primaryList;
                            List<DescriptionDocumentsObslugi> secondaryList;
                            if (applicantDocList.size() >= represDocList.size()) {
                                primaryList = applicantDocList;
                                secondaryList = represDocList;
                            } else {
                                primaryList = represDocList;
                                secondaryList = applicantDocList;
                            }

                            for (int i = 0; i < primaryList.size(); i++) {
                                //описание документа из главного списка
                                DescriptionDocumentsObslugi primDocDesc = primaryList.get(i);
                                //запомним позицию документа в главном списке
                                //нужно, чтобы синхронно доставать элемент из второстепенного списка
                                final int docNum = i;
                                final List<DescriptionDocumentsObslugi> finalSecondaryList = secondaryList;
                                Row.of(objectNotNull(rowIdx.getAndIncrement() + ref.getRow(), sheet::getRow, sheet::createRow), ref.getCol())
                                    //1
                                    .addCellWithValue(String.format("%d", rowIdx.get()))
                                    //2
                                    .addCellWithValue(SPACE)
                                    //3
                                    .addCellWithValue(Option.of(primDocDesc)
                                        .map(DescriptionDocumentsObslugi::getDocument)
                                        .map(Document::getDescription)
                                        .orElse(NO))
                                    //4
                                    .addCellWithValue(Option.of(primDocDesc)
                                        .map(DescriptionDocumentsObslugi::getRequirementsDocument)
                                        .orElse(NO))
                                    //5
                                    .addCellWithValue(SPACE)
                                    //6
                                    .addCellWithValue(SPACE)
                                    //7
                                    .addCellWithValue(() -> {
                                        if (docNum < finalSecondaryList.size()) {
                                            //описание документа из второстепенного списка
                                            DescriptionDocumentsObslugi secDocDesc = finalSecondaryList.get(docNum);
                                            return Option.of(secDocDesc)
                                                .map(DescriptionDocumentsObslugi::getDocument)
                                                .map(Document::getDescription)
                                                .orElse(HYPHEN);
                                        }
                                        return HYPHEN;
                                    })
                                    //8
                                    .addCellWithValue(() -> {
                                        if (docNum < finalSecondaryList.size()) {
                                            DescriptionDocumentsObslugi secDocDesc = finalSecondaryList.get(docNum);
                                            return Option.of(secDocDesc)
                                                .map(DescriptionDocumentsObslugi::getRequirementsDocument)
                                                .orElse(HYPHEN);
                                        }
                                        return HYPHEN;
                                    })
                                    //9
                                    .addCellWithValue(ZonedDateTime.now(ZoneId.of("Europe/Moscow")).format(DateTimeFormatter.ofPattern("dd.MM.uuuu", RUSSIAN)))
                                    //10
                                    .addCellWithValue(Option.of(service.getDescription()).orElse(NO))
                                    //11
                                    .addCellWithValue(Option.of(subService.getDescription()).orElse(NO))
                                    //12
                                    .addCellWithValue(
                                        ofAll(applicantDesc.getApplicantsCategorySubservice())
                                            .headOption()
                                            .map(PersonsEntitledReceivePodology::getDescription)
                                            .orElse(NO))
                                    //13
                                    .addCellWithValue(Option.of(primDocDesc)
                                        .map(DescriptionDocumentsObslugi::getParentDocument)
                                        .map(Document::getDescription)
                                        .orElse(NO))
                                    //14
                                    .addCellWithValue(
                                        Option.of(applicantDesc.getPossibilityApplication())
                                            .filter(v->v.getValue() != null)
                                            .map(v -> {
                                                if (v.getValue().toLowerCase().equals("имеется")) {
                                                    return "Имеется";
                                                } else {
                                                    return HYPHEN;
                                                }
                                            })
                                            .orElse(HYPHEN)
                                    )
                                    //15
                                    .addCellWithValue(
                                        ofAll(applicantDesc.getPerStatemBehApplicant())
                                            .flatMap(DescribeRepresentativesApplicants::getCatPerEntiRecObslu)
                                            .map(Representatives::getDescription)
                                            .reduceLeftOption(HSSFWorkbookTransformers::joiningNewLine)
                                            .orElse(HYPHEN)
                                    )
                                    //16
                                    .addCellWithValue(() -> {
                                        if (docNum < finalSecondaryList.size()) {
                                            DescriptionDocumentsObslugi secDocDesc = finalSecondaryList.get(docNum);
                                            return Option.of(secDocDesc)
                                                .map(DescriptionDocumentsObslugi::getParentDocument)
                                                .map(Document::getDescription)
                                                .orElse(NO);
                                        }
                                        return NO;
                                    })
                                ;
                            }
                        }
                    }
                }
            }
        }
        return workbook;
    }

    public static <Re extends SourceFututeHSSFWorkBookReport> HSSFWorkbook fromStreamServiceToFourthReport(Re report, Option<Service> serviceOption) {
        HSSFWorkbook workbook = report.getHSSFWorkbookFuture().get();
        CellReference ref = new CellReference(workbook.getName(report.getDataBagCellName()).getRefersToFormula());
        HSSFSheet sheet = workbook.getSheet(ref.getSheetName());
        AtomicInteger rowIdx = new AtomicInteger(0);
        if (serviceOption.isDefined()) {
            Service service = serviceOption.get();
            ofAll(service.getSubServices())
                .append(service.getObjAppeal())
                .filter(Objects::nonNull)
                .forEach(subService -> {
                    List<DescriptionDocumentsObslugi> subServiceInputDocs = subService.getFillDocSubservice().stream()
                        .collect(toList());
                    if (subServiceInputDocs.isEmpty()) {
                        Row.of(objectNotNull(rowIdx.getAndIncrement() + ref.getRow(), sheet::getRow, sheet::createRow), ref.getCol())
                            .addCellWithValue(SPACE) //1
                            .addCellWithValue(SPACE) //2
                            //3
                            .addCellWithValue(NO_DATA)
                            .addCellWithValue(SPACE) //4
                            .addCellWithValue(SPACE) //5
                            .addCellWithValue(SPACE) //6
                            .addCellWithValue(SPACE) //7
                            .addCellWithValue(SPACE) //8
                            //9
                            .addCellWithValue(ZonedDateTime.now(ZoneId.of("Europe/Moscow")).format(DateTimeFormatter.ofPattern("dd.MM.uuuu", RUSSIAN)))
                            //10
                            .addCellWithValue(Option.of(service.getDescription()).orElse(NO))
                            //11
                            .addCellWithValue(Option.of(subService.getDescription()).orElse(NO));
                    } else {
                        AtomicInteger docNum = new AtomicInteger(0);
                        subServiceInputDocs.forEach(docDesc ->
                            Row.of(objectNotNull(rowIdx.getAndIncrement() + ref.getRow(), sheet::getRow, sheet::createRow), ref.getCol())
                                //1
                                .addCellWithValue(String.format("%d", docNum.incrementAndGet()))
                                //2
                                .addCellWithValue(
                                    Option.of(docDesc.getParentDocument())
                                        .map(Document::getDescription).orElse(NO))
                                //3
                                .addCellWithValue(
                                    Option.of(docDesc.getDocument())
                                        .map(Document::getDescription).orElse(NO))
                                //4
                                .addCellWithValue(
                                    ofAll(
                                        Option.of(String.format("%d экз., %s", docDesc.getInstancesNumber(), docDesc.getTypeDocument().getValue())),
                                        ofAll(docDesc.getIdentificationApplicant(),
                                            docDesc.getVerificatOriginal(),
                                            docDesc.getRemovingCopy(),
                                            docDesc.getFormatCase())
                                            .filter(field -> field.getValue().orElse(false))
                                            .map(CMDField::getDescription),
                                        ofAll(docDesc.getActionsDocument()).map(ActionsDocument::getDescription)
                                    )
                                        .flatMap(identity())
                                        .reduceLeftOption(HSSFWorkbookTransformers::joiningNewLine).orElse(HYPHEN)
                                )
                                //5
                                .addCellWithValue(
                                    ofAll(docDesc.getRefCondition())
                                        .map(RefCondit::getRefCondition)
                                        .reduceLeftOption(HSSFWorkbookTransformers::joiningNewLine).orElse(NO))
                                //6
                                .addCellWithValue(Option.of(docDesc.getRequirementsDocument()).orElse(NO))
                                //7
                                //TODO: Переделать, когда появится механизм получения сведений о файлах
                                .addCellWithValue(
                                    Option.of(docDesc.getFormDocument())
                                        .map(FileList::getFiles)
                                        .filter(v -> !v.isEmpty())
                                        .map(v -> FILE_IS_ATTACHED)
                                        .orElse(FILE_IS_NOT_ATTACHED)
                                )
                                //8
                                //TODO: Переделать, когда появится механизм получения сведений о файлах
                                .addCellWithValue(
                                    Option.of(docDesc.getSampleDocument())
                                        .map(FileList::getFiles)
                                        .filter(v -> !v.isEmpty())
                                        .map(v -> FILE_IS_ATTACHED)
                                        .orElse(FILE_IS_NOT_ATTACHED)
                                )
                                //9
                                .addCellWithValue(ZonedDateTime.now(ZoneId.of("Europe/Moscow")).format(DateTimeFormatter.ofPattern("dd.MM.uuuu", RUSSIAN)))
                                //10
                                .addCellWithValue(Option.of(service.getDescription()).orElse(NO))
                                //11
                                .addCellWithValue(Option.of(subService.getDescription()).orElse(NO))
                        );
                    }
                });
        }
        return workbook;
    }

    public static <Re extends SourceFututeHSSFWorkBookReport> HSSFWorkbook fromStreamServiceToFifthReport(Re report, Option<Service> serviceOption) {
        HSSFWorkbook workbook = report.getHSSFWorkbookFuture().get();
        CellReference ref = new CellReference(workbook.getName(report.getDataBagCellName()).getRefersToFormula());
        HSSFSheet sheet = workbook.getSheet(ref.getSheetName());
        AtomicInteger rowIdx = new AtomicInteger(0);

        if (serviceOption.isDefined()) {
            Service service = serviceOption.get();
            List<SubServices> subServices = ofAll(service.getSubServices())
                .append(service.getObjAppeal())
                .filter(Objects::nonNull)
                .toJavaList();
            for (SubServices subService : subServices) {
                List<DescriptionDocumentsObslugi> docList = ofAll(subService)
                    .flatMap(SubServices::getFillDocSubservice)
                    .filter(doc -> !doc.getRefMVRequests().isEmpty())
                    .toJavaList();
                if (docList.isEmpty()) {
                    Row.of(objectNotNull(rowIdx.getAndIncrement() + ref.getRow(), sheet::getRow, sheet::createRow), ref.getCol())
                        .addCellWithValue(SPACE) //1
                        //2
                        .addCellWithValue(NO_DATA)
                        .addCellWithValue(SPACE) //3
                        .addCellWithValue(SPACE) //4
                        .addCellWithValue(SPACE) //5
                        .addCellWithValue(SPACE) //6
                        .addCellWithValue(SPACE) //7
                        .addCellWithValue(SPACE) //8
                        .addCellWithValue(SPACE) //9
                        .addCellWithValue(SPACE) //10
                        //11
                        .addCellWithValue(ZonedDateTime.now(ZoneId.of("Europe/Moscow")).format(DateTimeFormatter.ofPattern("dd.MM.uuuu", RUSSIAN)))
                        //12
                        .addCellWithValue(Option.of(service.getDescription()).orElse(NO))
                        //13
                        .addCellWithValue(Option.of(subService.getDescription()).orElse(NO));
                } else {
                    ofAll(docList).forEach(docDesc ->
                        Row.of(objectNotNull(rowIdx.getAndIncrement() + ref.getRow(), sheet::getRow, sheet::createRow), ref.getCol())
                            .addCellWithValue(SPACE) //1
                            //2
                            .addCellWithValue(ofAll(service.getNalichieApprovedTKMW())
                                .headOption()
                                .map(DescriptionTKMW::getTypDoTKMW)
                                .orElse(NO)
                            )
                            //3
                            .addCellWithValue(Option.of(docDesc.getDescription()).orElse(NO))
                            //4
                            .addCellWithValue(
                                ofAll(docDesc.getRefMVRequests())
                                    .filter(Objects::nonNull)
                                    .map(MVrequests::getInformationContent)
                                    .reduceLeftOption(HSSFWorkbookTransformers::joiningNewLine)
                                    .orElse(NO)
                            )
                            //5
                            .addCellWithValue(
                                ofAll(docDesc.getRefMVRequests())
                                    .filter(Objects::nonNull)
                                    .map(MVrequests::getNamOrgGuiInteRequest)
                                    .filter(Objects::nonNull)
                                    .map(OrgGovernment::getFullname)
                                    .map("- "::concat)
                                    .reduceLeftOption(HSSFWorkbookTransformers::joiningNewLine)
                                    .orElse(NO)
                            )
                            //6
                            .addCellWithValue(
                                ofAll(docDesc.getRefMVRequests())
                                    .filter(Objects::nonNull)
                                    .map(MVrequests::getMamAuthSeInteReq)
                                    .filter(Objects::nonNull)
                                    .map(OrgGovernment::getFullname)
                                    .map("- "::concat)
                                    .reduceLeftOption(HSSFWorkbookTransformers::joiningNewLine)
                                    .orElse(NO)
                            )
                            //7
                            .addCellWithValue(
                                ofAll(docDesc.getRefMVRequests())
                                    .filter(Objects::nonNull)
                                    .map(MVrequests::getSidElectService)
                                    .filter(Objects::nonNull)
                                    .map("- "::concat)
                                    .reduceLeftOption(HSSFWorkbookTransformers::joiningNewLine)
                                    .orElse(NO)
                            )
                            //8
                            .addCellWithValue(
                                ofAll(docDesc.getRefMVRequests())
                                    .filter(Objects::nonNull)
                                    .map(request -> {
                                        String cellText = EMPTY;
                                        if (request.getRequestExecutionTerm() != null && request.getUnitAllrequest() != null) {
                                            if (!cellText.equals(EMPTY)) {
                                                cellText += "\n";
                                            }
                                            cellText += String.format("Общий срок осуществления межведомственного информационного взаимодействия\n%1$d %2$s",
                                                request.getRequestExecutionTerm(),
                                                request.getUnitAllrequest().getValue());
                                        }
                                        if (request.getRequestingMV() != null && request.getUnitrequestingMV() != null) {
                                            if (!cellText.equals(EMPTY)) {
                                                cellText += "\n";
                                            }
                                            cellText += String.format("Сроки направления межведомственного запроса\n%1$s %2$s",
                                                request.getRequestingMV(),
                                                request.getUnitrequestingMV().getValue());

                                        }
                                        if (request.getDirectionResponse() != null && request.getUnitdirectionResponse() != null) {
                                            if (!cellText.equals(EMPTY)) {
                                                cellText += "\n";
                                            }
                                            cellText += String.format("Сроки направления ответа на межведомственный запрос\n%1$s %2$s",
                                                request.getDirectionResponse(),
                                                request.getUnitdirectionResponse().getValue());

                                        }
                                        if (request.getCommunionResponse() != null && request.getComResponse() != null) {
                                            if (!cellText.equals(EMPTY)) {
                                                cellText += "\n";
                                            }
                                            cellText += String.format("Сроки приобщения документов/сведений, " +
                                                    "полученных в рамках межведомственного информационного взаимодействия, " +
                                                    "к личному делу заявителя\n%1$s %2$s",
                                                request.getCommunionResponse(),
                                                request.getComResponse().getValue());

                                        }
                                        return cellText;
                                    })
                                    .reduceLeftOption(HSSFWorkbookTransformers::joiningNewLine)
                                    .orElse(NO)
                            )
                            //9
                            //TODO Переделать, когда появится механизм получения сведений о файлах
                            .addCellWithValue(
                                ofAll(docDesc.getRefMVRequests())
                                    .filter(Objects::nonNull)
                                    .map(MVrequests::getFileFormInReqText)
                                    .filter(Objects::nonNull)
                                    .map(FileList::getFiles)
                                    .filter(Objects::nonNull)
                                    .map(v -> {
                                        if (v.isEmpty()) {
                                            return FILE_IS_NOT_ATTACHED;
                                        } else {
                                            return FILE_IS_ATTACHED;
                                        }
                                    })
                                    .map("- "::concat)
                                    .reduceLeftOption(HSSFWorkbookTransformers::joiningNewLine)
                                    .orElse(FILE_IS_NOT_ATTACHED)
                            )
                            //10
                            //TODO Переделать, когда появится механизм получения сведений о файлах
                            .addCellWithValue(
                                ofAll(docDesc.getRefMVRequests())
                                    .filter(Objects::nonNull)
                                    .map(MVrequests::getFileFillPatText)
                                    .filter(Objects::nonNull)
                                    .map(FileList::getFiles)
                                    .filter(Objects::nonNull)
                                    .map(v -> {
                                        if (v.isEmpty()) {
                                            return FILE_IS_NOT_ATTACHED;
                                        } else {
                                            return FILE_IS_ATTACHED;
                                        }
                                    })
                                    .map("- "::concat)
                                    .reduceLeftOption(HSSFWorkbookTransformers::joiningNewLine)
                                    .orElse(FILE_IS_NOT_ATTACHED)
                            )
                            //11
                            .addCellWithValue(ZonedDateTime.now(ZoneId.of("Europe/Moscow")).format(DateTimeFormatter.ofPattern("dd.MM.uuuu", RUSSIAN)))
                            //12
                            .addCellWithValue(Option.of(service.getDescription()).orElse(NO))
                            //13
                            .addCellWithValue(Option.of(subService.getDescription()).orElse(NO))
                    );
                }
            }
        }
        return workbook;
    }

    public static <Re extends SourceFututeHSSFWorkBookReport> HSSFWorkbook fromStreamServiceToSixthReport(Re report, Option<Service> serviceOption) {
        HSSFWorkbook workbook = report.getHSSFWorkbookFuture().get();
        CellReference ref = new CellReference(workbook.getName(report.getDataBagCellName()).getRefersToFormula());
        HSSFSheet sheet = workbook.getSheet(ref.getSheetName());
        AtomicInteger rowIdx = new AtomicInteger(0);
        if (serviceOption.isDefined()) {
            Service service = serviceOption.get();
            ofAll(service.getSubServices())
                .append(service.getObjAppeal())
                .filter(Objects::nonNull)
                .forEach(subService -> {
                    List<DescriptionDocumentsObslugi> subServiceResultDocs = subService.getFillResultSubservice().stream().collect(toList());
                    if (subServiceResultDocs.isEmpty()) {
                        Row.of(objectNotNull(rowIdx.getAndIncrement() + ref.getRow(), sheet::getRow, sheet::createRow), ref.getCol())
                            .addCellWithValue(SPACE) //1
                            .addCellWithValue(NO_DATA) //2
                            .addCellWithValue(SPACE) //3
                            .addCellWithValue(SPACE) //4
                            .addCellWithValue(SPACE) //5
                            .addCellWithValue(SPACE) //6
                            .addCellWithValue(SPACE) //7
                            .addCellWithValue(SPACE) //8
                            //9
                            .addCellWithValue(SPACE)
                            //10
                            .addCellWithValue(ZonedDateTime.now(ZoneId.of("Europe/Moscow")).format(DateTimeFormatter.ofPattern("dd.MM.uuuu", RUSSIAN)))
                            //11
                            .addCellWithValue(Option.of(service.getDescription()).orElse(NO))
                            //12
                            .addCellWithValue(Option.of(subService.getDescription()).orElse(NO));
                    } else {
                        AtomicInteger docNum = new AtomicInteger(0);
                        subServiceResultDocs.forEach(resDesc ->
                            Row.of(objectNotNull(rowIdx.getAndIncrement() + ref.getRow(), sheet::getRow, sheet::createRow), ref.getCol())
                                //1
                                .addCellWithValue(String.format("%d", docNum.incrementAndGet()))
                                //2
                                .addCellWithValue(
                                    Option.of(resDesc.getDescription()).orElse(NO))
                                //3
                                .addCellWithValue(
                                    Option.of(resDesc.getRequirements()).orElse(NO))
                                //4
                                .addCellWithValue(
                                    Option.of(resDesc.getCharacterOfResult())
                                        .map(Lookup::getValue).orElse(NO)
                                )
                                //5
                                //TODO Переделать, когда появится механизм получения сведений о файлах
                                .addCellWithValue(
                                    Option.of(resDesc.getDocForm())
                                        .map(FileList::getFiles)
                                        .filter(v -> !v.isEmpty())
                                        .map(v -> FILE_IS_ATTACHED)
                                        .orElse(FILE_IS_NOT_ATTACHED)
                                )
                                //6
                                //TODO Переделать, когда появится механизм получения сведений о файлах
                                .addCellWithValue(
                                    Option.of(resDesc.getDocExample())
                                        .map(FileList::getFiles)
                                        .filter(v -> !v.isEmpty())
                                        .map(v -> FILE_IS_ATTACHED)
                                        .orElse(FILE_IS_NOT_ATTACHED)
                                )
                                //7
                                .addCellWithValue(
                                    ofAll(
                                        ofAll(
                                            resDesc.getTerrOrgOnPaper(),
                                            resDesc.getAuthoOnPaper(),
                                            resDesc.getInMFConPaperFrom(),
                                            resDesc.getInMFCinDocFromITOrg(),
                                            resDesc.getFromCabinetGosUslug(),
                                            resDesc.getFromGosUslugInELForm())
                                            .filter(field -> field.getValue().orElse(false))
                                            .map(CMDField::getDescription),
                                        Option.of(
                                            resDesc.getFromCabinetOffSite())
                                            .filter(field -> field.getValue()
                                                .filter(value -> value.equals(true))
                                                .isDefined())
                                            .map(addr -> String.format("Адрес сайта %s", resDesc.getAddresOffSiteResult())),
                                        Option.of(
                                            resDesc.getFromOffSiteElDoc())
                                            .filter(field -> field.getValue()
                                                .filter(value -> value.equals(true))
                                                .isDefined())
                                            .map(addr -> String.format("Адрес сайта %s", resDesc.getAddresOffSiteELDoc())),
                                        ofAll(
                                            resDesc.getEmailDocWithElSignature(),
                                            resDesc.getPostResult())
                                            .filter(field -> field.getValue().orElse(false))
                                            .map(CMDField::getDescription),
                                        ofAll(
                                            resDesc.getGetRezultSubServices())
                                            .filter(field -> field != null)
                                            .map(SubserviceResult::getDescription)
                                    )
                                        .flatMap(Function.identity())
                                        .map("- "::concat)
                                        .reduceLeftOption(HSSFWorkbookTransformers::joiningWithSemicolonAndNewLine)
                                        .orElse(HYPHEN)
                                )
                                //8
                                .addCellWithValue(
                                    Option.of(resDesc)
                                        .filter(result ->
                                            result.getNumberOfDays() != null &&
                                                result.getUnitOfMeasure1() != null)
                                        .map(result -> String.format("%s %s",
                                            result.getNumberOfDays(),
                                            result.getUnitOfMeasure1().getValue()))
                                        .orElse(NO)
                                )
                                //9
                                .addCellWithValue(
                                    Option.of(resDesc)
                                        .filter(result ->
                                            result.getNumOfDays() != null &&
                                                result.getUnitOfMeas1() != null)
                                        .map(result -> String.format("%s %s",
                                            result.getNumOfDays(),
                                            result.getUnitOfMeas1().getValue()))
                                        .orElse(NO)
                                )
                                //10
                                .addCellWithValue(ZonedDateTime.now(ZoneId.of("Europe/Moscow")).format(DateTimeFormatter.ofPattern("dd.MM.uuuu", RUSSIAN)))
                                //11
                                .addCellWithValue(Option.of(service.getDescription()).orElse(NO))
                                //12
                                .addCellWithValue(Option.of(subService.getDescription()).orElse(NO))
                        );
                    }
                });
        }
        return workbook;
    }

    public static <Re extends SourceFututeHSSFWorkBookReport> HSSFWorkbook fromStreamServiceToSeventhReport(Re report, Option<Service> serviceOption) {
        HSSFWorkbook workbook = report.getHSSFWorkbookFuture().get();
        CellReference ref = new CellReference(workbook.getName(report.getDataBagCellName()).getRefersToFormula());
        HSSFSheet sheet = workbook.getSheet(ref.getSheetName());
        AtomicInteger rowIdx = new AtomicInteger(0);

        if (serviceOption.isDefined()) {
            Service service = serviceOption.get();
            ofAll(service.getSubServices())
                .append(service.getObjAppeal())
                .filter(Objects::nonNull)
                .forEach(subService -> {
                    List<ProcessesSubservices> processSubserviceList = subService.getFillProcessSubservice();
                    if (processSubserviceList.isEmpty()) {
                        Row.of(objectNotNull(rowIdx.getAndIncrement() + ref.getRow(), sheet::getRow, sheet::createRow), ref.getCol())
                            .addCellWithValue(SPACE) //1
                            .addCellWithValue(NO_DATA) //2
                            .addCellWithValue(SPACE) //3
                            .addCellWithValue(SPACE) //4
                            .addCellWithValue(SPACE) //5
                            .addCellWithValue(SPACE) //6
                            .addCellWithValue(SPACE) //7
                            //8
                            .addCellWithValue(ZonedDateTime.now(ZoneId.of("Europe/Moscow")).format(DateTimeFormatter.ofPattern("dd.MM.uuuu", RUSSIAN)))
                            //9
                            .addCellWithValue(Option.of(service.getDescription()).orElse(NO))
                            //10
                            .addCellWithValue(Option.of(subService.getDescription()).orElse(NO))
                            .addCellWithValue(NO_DATA); //11
                    } else {
                        processSubserviceList.forEach(techProc -> {
                            List<AdministrativeProcedures> adminProcedureList = techProc.getListAdminProcedures();
                            List<DescriptiProcess> procedProcessList = techProc.getNamProcedProcess();
                            Map<AdministrativeProcedures, List<DescriptiProcess>> procedureToProcessMap = new HashMap<>();
                            for (AdministrativeProcedures adminProc : adminProcedureList) {
                                List<DescriptiProcess> list = new ArrayList<>();
                                for (DescriptiProcess processDesc : procedProcessList) {
                                    if (adminProc.equals(processDesc.getBelongAdminProced2())) {
                                        list.add(processDesc);
                                    }
                                }
                                procedureToProcessMap.put(adminProc, list);
                            }

                            procedureToProcessMap.forEach((adminProcedure, processList) -> {
                                if (processList.isEmpty()) {
                                    Row.of(objectNotNull(rowIdx.getAndIncrement() + ref.getRow(), sheet::getRow, sheet::createRow), ref.getCol())
                                        .addCellWithValue(SPACE) //1
                                        .addCellWithValue(NO_DATA) //2
                                        .addCellWithValue(SPACE) //3
                                        .addCellWithValue(SPACE) //4
                                        .addCellWithValue(SPACE) //5
                                        .addCellWithValue(SPACE) //6
                                        .addCellWithValue(SPACE) //7
                                        //8
                                        .addCellWithValue(ZonedDateTime.now(ZoneId.of("Europe/Moscow")).format(DateTimeFormatter.ofPattern("dd.MM.uuuu", RUSSIAN)))
                                        //9
                                        .addCellWithValue(Option.of(service.getDescription()).orElse(NO))
                                        //10
                                        .addCellWithValue(Option.of(subService.getDescription()).orElse(NO))
                                        //11
                                        .addCellWithValue(Option.of(adminProcedure)
                                            .map(AdministrativeProcedures::getDescription)
                                            .orElse(NO_DATA));
                                } else {
                                    processList.forEach(procedureProcess ->
                                        Row.of(objectNotNull(rowIdx.getAndIncrement() + ref.getRow(), sheet::getRow, sheet::createRow), ref.getCol())
                                            //1
                                            .addCellWithValue(() ->
                                                String.format("%d", processList.indexOf(procedureProcess) + 1)
                                            )
                                            //2
                                            .addCellWithValue(
                                                Option.of(procedureProcess.getDescription()).orElse(NO)
                                            )
                                            //3
                                            .addCellWithValue(
                                                Option.of(procedureProcess.getDetailsProcProcess()).orElse(NO)
                                            )
                                            //4
                                            .addCellWithValue(
                                                ofAll(
                                                    Option.of(procedureProcess.getPeriodApplyDays())
                                                        .map(v -> String.format("%d", v))
                                                        .orElse(EMPTY),
                                                    Option.of(procedureProcess.getLookupClock())
                                                        .map(Lookup::getValue).orElse(EMPTY),
                                                    Option.of(procedureProcess.getLookupTimePeriod())
                                                        .map(Lookup::getValue).orElse(EMPTY)
                                                )
                                                    .filter(v -> !v.equals(EMPTY))
                                                    .reduceLeftOption(HSSFWorkbookTransformers::joiningNewLine)
                                                    .orElse(NO)
                                            )
                                            //5
                                            .addCellWithValue(
                                                Option.of(procedureProcess.getIspProcedur())
                                                    .map(PerformersProcedure::getDescription)
                                                    .orElse(NO)
                                            )
                                            //6
                                            .addCellWithValue(
                                                Option.of(procedureProcess.getResources()).orElse(NO)
                                            )
                                            //7
                                            //TODO Переделать, когда появится механизм получения сведений о файлах
                                            .addCellWithValue(
                                                ofAll(procedureProcess.getFormsDocuments())
                                                    .map(docDesc -> {
                                                        String cellText = EMPTY;
                                                        Option<FileList> docFormOptional = Option.of(docDesc.getDocForm());
                                                        cellText += docFormOptional
                                                            .map(FileList::getFiles)
                                                            .filter(List::isEmpty)
                                                            .map(v -> "Форма приложена")
                                                            .orElse("Форма не приложена");
                                                        if (!cellText.isEmpty()) {
                                                            cellText += "\n";
                                                        }
                                                        Option<FileList> docExmapleOptionoal = Option.of(docDesc.getDocExample());
                                                        cellText += docExmapleOptionoal
                                                            .map(FileList::getFiles)
                                                            .filter(List::isEmpty)
                                                            .map(v -> "Образец приложен")
                                                            .orElse("Образец не приложен");

                                                        return cellText;
                                                    })
                                                    .reduceLeftOption(HSSFWorkbookTransformers::joiningNewLine)
                                                    .orElse(FILE_IS_NOT_ATTACHED)
                                            )
                                            //8
                                            .addCellWithValue(ZonedDateTime.now(ZoneId.of("Europe/Moscow")).format(DateTimeFormatter.ofPattern("dd.MM.uuuu", RUSSIAN)))
                                            //9
                                            .addCellWithValue(Option.of(service.getDescription()).orElse(NO))
                                            //10
                                            .addCellWithValue(Option.of(subService.getDescription()).orElse(NO))
                                            //11
                                            .addCellWithValue(Option.of(adminProcedure)
                                                .map(AdministrativeProcedures::getDescription)
                                                .orElse(NO_DATA)));
                                }
                            });
                        });
                    }
                });
        }
        return workbook;
    }

    public static <Re extends SourceFututeHSSFWorkBookReport> HSSFWorkbook fromStreamServiceToEighthReport(Re report, Option<Service> serviceOption) {
        HSSFWorkbook workbook = report.getHSSFWorkbookFuture().get();
        CellReference ref = new CellReference(workbook.getName(report.getDataBagCellName()).getRefersToFormula());
        HSSFSheet sheet = workbook.getSheet(ref.getSheetName());
        AtomicInteger rowIdx = new AtomicInteger(0);
        if (serviceOption.isDefined()) {
            Service service = serviceOption.get();
            ofAll(service.getSubServices())
                .append(service.getObjAppeal())
                .filter(Objects::nonNull)
                .forEach(subService ->
                    Row.of(objectNotNull(rowIdx.getAndIncrement() + ref.getRow(), sheet::getRow, sheet::createRow), ref.getCol())
                        //1
                        .addCellWithValue(SPACE)
                        //2
                        .addCellWithValue(ofAll(
                            Option.of(subService.getOfficialWebsite())
                                .filter(field -> field.getValue().orElse(false))
                                .map(v -> String.format("Адрес сайта: %s",
                                    Option.of(subService.getWebAddress()).orElse(EMPTY))),
                            Option.of(subService.getPortalStateServices())
                                .filter(field -> field.getValue().orElse(false))
                                .map(CMDField::getDescription),
                            ofAll(subService.getOtherInformaTerms())
                                .filter(Objects::nonNull)
                                .map(ProductInformatSubser::getOtherWays)
                                .filter(Objects::nonNull))
                            .flatMap(identity())
                            .reduceLeftOption(HSSFWorkbookTransformers::joiningWithSemicolonAndNewLine)
                            .orElse(NO))
                        //3
                        .addCellWithValue(ofAll(
                            Option.of(subService.getOfficialWebsite())
                                .filter(field -> field.getValue().orElse(false))
                                .map(v -> String.format("Адрес сайта: %s",
                                    Option.of(subService.getWebAddress()).orElse(EMPTY))),
                            Option.of(subService.getPortalStateSrvices())
                                .filter(field -> field.getValue().orElse(false))
                                .map(CMDField::getDescription),
                            ofAll(subService.getMethoAppointBodies())
                                .filter(Objects::nonNull)
                                .map(OtheMethoAppointm::getOtherMethodAppointment)
                                .filter(Objects::nonNull))
                            .flatMap(identity())
                            .reduceLeftOption(HSSFWorkbookTransformers::joiningWithSemicolonAndNewLine)
                            .orElse(NO))
                        //4
                        .addCellWithValue(ofAll(
                            ofAll(
                                subService.getRequirPapeDocuments(),
                                subService.getRequireDocumenPaper(),
                                subService.getSubmiDocObtaiResult())
                                .filter(field -> field.getValue().orElse(false))
                                .map(CMDField::getDescription),
                            ofAll(subService.getOtherReception())
                                .filter(Objects::nonNull)
                                .map(WayReceptRequest::getOthRecRegisteringDocuments)
                                .filter(Objects::nonNull))
                            .flatMap(identity())
                            .reduceLeftOption(HSSFWorkbookTransformers::joiningWithSemicolonAndNewLine)
                            .orElse(NO))
                        //5
                        .addCellWithValue(ofAll(
                            Option.of(subService.getOfficialWebProvidiService())
                                .filter(field -> field.getValue().orElse(false))
                                .map(v -> String.format("Адрес сайта: %s",
                                    Option.of(subService.getWebsitAddrePayMethods()).orElse(EMPTY))),
                            Option.of(subService.getPublServPayMeth())
                                .filter(field -> field.getValue().orElse(false))
                                .map(CMDField::getDescription),
                            ofAll(subService.getOtheMethoPaymStaFee())
                                .filter(Objects::nonNull)
                                .map(OtheMethodPaymen::getMethoPymeStatFee)
                                .filter(Objects::nonNull))
                            .flatMap(identity())
                            .reduceLeftOption(HSSFWorkbookTransformers::joiningWithSemicolonAndNewLine)
                            .orElse(NO))
                        //6
                        .addCellWithValue(ofAll(
                            Option.of(subService.getOfficlWebMetInform())
                                .filter(field -> field.getValue().orElse(false))
                                .map(v -> String.format("Адрес сайта: %s",
                                    Option.of(subService.getWebsitAddresInformation()).orElse(EMPTY))),
                            ofAll(subService.getServicePortaInformation(),
                                subService.getEmailApplicant())
                                .filter(field -> field.getValue().orElse(false))
                                .map(CMDField::getDescription),
                            ofAll(subService.getOthMeInforAboCourse())
                                .filter(Objects::nonNull)
                                .map(MethObtProgresInfo::getObtaInfoProgRequest)
                                .filter(Objects::nonNull))
                            .flatMap(identity())
                            .reduceLeftOption(HSSFWorkbookTransformers::joiningWithSemicolonAndNewLine)
                            .orElse(NO))
                        //7
                        .addCellWithValue(ofAll(
                            Option.of(subService.getOfficlWebCompla())
                                .filter(field -> field.getValue().orElse(false))
                                .map(v -> String.format("Адрес сайта: %s",
                                    Option.of(subService.getAddresComplaint()).orElse(EMPTY))),
                            ofAll(subService.getServicPortComplaints(),
                                subService.getInfoSystEnsurDecisions())
                                .filter(field -> field.getValue().orElse(false))
                                .map(CMDField::getDescription),
                            ofAll(subService.getOthFlinComplaint())
                                .filter(Objects::nonNull)
                                .map(OthWayFiliComplaint::getWayFilinComplaint)
                                .filter(Objects::nonNull))
                            .flatMap(identity())
                            .reduceLeftOption(HSSFWorkbookTransformers::joiningWithSemicolonAndNewLine)
                            .orElse(NO))
                        //8
                        .addCellWithValue(ZonedDateTime.now(ZoneId.of("Europe/Moscow")).format(DateTimeFormatter.ofPattern("dd.MM.uuuu", RUSSIAN)))
                        //9
                        .addCellWithValue(Option.of(service.getDescription()).orElse(NO))
                        //10
                        .addCellWithValue(Option.of(subService.getDescription()).orElse(NO))
                        //11
                        .addCellWithValue(String.format("%d", rowIdx.get()))
                );
        }
        return workbook;
    }

    public static <Re extends SourceFututeHSSFWorkBookReport> HSSFWorkbook fromOptionIncidentToReportMessage(Re report, Option<Incident> incidentOption) {
        HSSFWorkbook workbook = report.getHSSFWorkbookFuture().get();
        CellReference ref = new CellReference(workbook.getName(report.getDataBagCellName()).getRefersToFormula());
        HSSFSheet sheet = workbook.getSheet(ref.getSheetName());
        AtomicInteger rowIdx = new AtomicInteger(0);
        if (incidentOption.isDefined()) {
            Incident incident = incidentOption.get();
            //1
            Row.of(objectNotNull(rowIdx.getAndIncrement() + ref.getRow(), sheet::getRow, sheet::createRow), ref.getCol())
                .addCellWithValue("1")
                .addCellWithValue("ОСТ")
                .addCellWithValue(Option.of(incident.getOst()).orElse(HYPHEN));
            //2
            Row.of(objectNotNull(rowIdx.getAndIncrement() + ref.getRow(), sheet::getRow, sheet::createRow), ref.getCol())
                .addCellWithValue("2")
                .addCellWithValue("РНУ")
                .addCellWithValue(Option.of(incident.getRnu()).orElse(HYPHEN));
            //3
            Row.of(objectNotNull(rowIdx.getAndIncrement() + ref.getRow(), sheet::getRow, sheet::createRow), ref.getCol())
                .addCellWithValue("3")
                .addCellWithValue("Нефтепровод (наименование, километр, диаметр, max разрешенное давление " +
                    "и давление на момент отказа/аварии/инцидента, год ввода МН,  в эксплуатацию, " +
                    "толщина стенки, марка стали)")
                .addCellWithValue(Option.of(incident.getPipeline()).orElse(HYPHEN));
            //4
            Row.of(objectNotNull(rowIdx.getAndIncrement() + ref.getRow(), sheet::getRow, sheet::createRow), ref.getCol())
                .addCellWithValue("4")
                .addCellWithValue("Технологический участок, диаметр")
                .addCellWithValue(Option.of(incident.getTu()).orElse(HYPHEN));
            //5
            Row.of(objectNotNull(rowIdx.getAndIncrement() + ref.getRow(), sheet::getRow, sheet::createRow), ref.getCol())
                .addCellWithValue("5")
                .addCellWithValue("Местоположение (республика, край, область, район, населенный пункт, " +
                    "ближайший водный объект, авто- и ж/д дороги)")
                .addCellWithValue(Option.of(incident.getLocationEvent()).orElse(HYPHEN));
            //6
            Row.of(objectNotNull(rowIdx.getAndIncrement() + ref.getRow(), sheet::getRow, sheet::createRow), ref.getCol())
                .addCellWithValue("6")
                .addCellWithValue("Дата и время отказа, повреждения запорной арматуры")
                .addCellWithValue(
                    Option.of(incident.getEventTimeDate())
                        .map(v -> String.format(RUSSIAN, "%1$td.%1$tm.%1$tY %1$TR", v))
                        .orElse(HYPHEN)
                );
            //7
            Row.of(objectNotNull(rowIdx.getAndIncrement() + ref.getRow(), sheet::getRow, sheet::createRow), ref.getCol())
                .addCellWithValue("7")
                .addCellWithValue("Тип запорной арматуры DN, РN, технологический номер, завод изготовитель")
                .addCellWithValue(Option.of(incident.getValves()).orElse(HYPHEN));
            //8
            Row.of(objectNotNull(rowIdx.getAndIncrement() + ref.getRow(), sheet::getRow, sheet::createRow), ref.getCol())
                .addCellWithValue("8")
                .addCellWithValue("Дата изготовления запорной арматуры, дата ввода в эксплуатацию, " +
                    "дата капитального ремонта")
                .addCellWithValue(String.format("Дата изготовления - %1s\n" +
                        "Дата ввода в эксплуатацию - %2s\n" +
                        "Дата капитального ремонта - %3s",
                    Option.of(incident.getValvesCreate())
                        .map(v -> String.format("%1$td.%1$tm.%1$tY", v))
                        .orElse("нет данных"),
                    Option.of(incident.getValvesStart())
                        .map(v -> String.format("%1$td.%1$tm.%1$tY", v))
                        .orElse("нет данных"),
                    Option.of(incident.getValvesKapRem())
                        .map(v -> String.format("%1$td.%1$tm.%1$tY", v))
                        .orElse("нет данных")
                ));
            //9
            Row.of(objectNotNull(rowIdx.getAndIncrement() + ref.getRow(), sheet::getRow, sheet::createRow), ref.getCol())
                .addCellWithValue("9")
                .addCellWithValue("Дата проведения последнего ТО, ТР, СР, диагностического обследования. " +
                    "Результат обследования: (продлён/не продлён срок службы до (даты), " +
                    "выявленные/устранённые дефекты)")
                .addCellWithValue(Option.of(incident.getLastTO()).orElse(HYPHEN));
            //10
            Row.of(objectNotNull(rowIdx.getAndIncrement() + ref.getRow(), sheet::getRow, sheet::createRow), ref.getCol())
                .addCellWithValue("10")
                .addCellWithValue("Дата проведения проверки запорной арматуры на полное открытие-закрытие, " +
                    "наличие соответствующего акта")
                .addCellWithValue(Option.of(incident.getLastCheck()).orElse(HYPHEN));
            //11
            Row.of(objectNotNull(rowIdx.getAndIncrement() + ref.getRow(), sheet::getRow, sheet::createRow), ref.getCol())
                .addCellWithValue("11")
                .addCellWithValue("Марка электропривода")
                .addCellWithValue(Option.of(incident.getDriverBrand()).orElse(HYPHEN));
            //12
            Row.of(objectNotNull(rowIdx.getAndIncrement() + ref.getRow(), sheet::getRow, sheet::createRow), ref.getCol())
                .addCellWithValue("12")
                .addCellWithValue("Причины отказа, повреждения запорной арматуры")
                .addCellWithValue(Option.of(incident.getIncReason()).orElse(HYPHEN));
            //13
            Row.of(objectNotNull(rowIdx.getAndIncrement() + ref.getRow(), sheet::getRow, sheet::createRow), ref.getCol())
                .addCellWithValue("13")
                .addCellWithValue("Ближайшие НПС, какими задвижками (номер, километр) и во сколько отсечен участок")
                .addCellWithValue(Option.of(incident.getNearNPS()).orElse(HYPHEN));
            //14
            Row.of(objectNotNull(rowIdx.getAndIncrement() + ref.getRow(), sheet::getRow, sheet::createRow), ref.getCol())
                .addCellWithValue("14")
                .addCellWithValue("Источник обнаружения:");
            //14.1
            Row.of(objectNotNull(rowIdx.getAndIncrement() + ref.getRow(), sheet::getRow, sheet::createRow), ref.getCol())
                .addCellWithValue("14.1")
                .addCellWithValue("Обслуживающий персонал, служба безопасности")
                .addCellWithValue(Option.of(incident.getEmployee()).orElse(HYPHEN));
            //14.2
            Row.of(objectNotNull(rowIdx.getAndIncrement() + ref.getRow(), sheet::getRow, sheet::createRow), ref.getCol())
                .addCellWithValue("14.2")
                .addCellWithValue("Данные СДКУ (ЕСУ) по снижению давления\n" +
                    "с __ кгс/см2  до ___ кгс/см2 \n" +
                    "(на ___ кгс/см2 от расчетного значения)")
                .addCellWithValue(Option.of(incident.getSdku()).orElse(HYPHEN));
            //14.3
            Row.of(objectNotNull(rowIdx.getAndIncrement() + ref.getRow(), sheet::getRow, sheet::createRow), ref.getCol())
                .addCellWithValue("14.3")
                .addCellWithValue("Стороннее лицо (Ф.И.О. тел.)")
                .addCellWithValue(Option.of(incident.getOtherPerson()).orElse(HYPHEN));
            //15
            Row.of(objectNotNull(rowIdx.getAndIncrement() + ref.getRow(), sheet::getRow, sheet::createRow), ref.getCol())
                .addCellWithValue("15")
                .addCellWithValue("Дата и время выезда патрульной группы, " +
                    "продолжительность обследования (нормативное)")
                .addCellWithValue(Option.of(incident.getReactionTime()).orElse(HYPHEN));
            //17
            Row.of(objectNotNull(rowIdx.getAndIncrement() + ref.getRow(), sheet::getRow, sheet::createRow), ref.getCol())
                .addCellWithValue("17")
                .addCellWithValue("Фактические дата и время обнаружения (аварии/инцидента/ отказа) патрульной группой, " +
                    "продолжительность обследования фактическая " +
                    "(при увеличении времени обследования от нормативного указать причины)")
                .addCellWithValue(Option.of(incident.getReactionTimeFact()).orElse(HYPHEN));
            //18
            Row.of(objectNotNull(rowIdx.getAndIncrement() + ref.getRow(), sheet::getRow, sheet::createRow), ref.getCol())
                .addCellWithValue("18")
                .addCellWithValue("Время простоя, сокращения перекачки")
                .addCellWithValue(Option.of(incident.getDown()).orElse(HYPHEN));
            //19
            Row.of(objectNotNull(rowIdx.getAndIncrement() + ref.getRow(), sheet::getRow, sheet::createRow), ref.getCol())
                .addCellWithValue("19")
                .addCellWithValue("Снижение объемов перекачки нефти")
                .addCellWithValue(Option.of(incident.getDownValue()).orElse(HYPHEN));
            //20
            Row.of(objectNotNull(rowIdx.getAndIncrement() + ref.getRow(), sheet::getRow, sheet::createRow), ref.getCol())
                .addCellWithValue("20")
                .addCellWithValue("Планируемые способ и сроки устранения отказа, повреждения")
                .addCellWithValue(Option.of(incident.getPlanRepair()).orElse(HYPHEN));
            //21
            Row.of(objectNotNull(rowIdx.getAndIncrement() + ref.getRow(), sheet::getRow, sheet::createRow), ref.getCol())
                .addCellWithValue("21")
                .addCellWithValue("Ф.И.О. лица, возглавляющего работы по ликвидации отказа, " +
                    "повреждения, должность, контактный телефон")
                .addCellWithValue(Option.of(incident.getLikvidator()).orElse(HYPHEN));
            //22
            Row.of(objectNotNull(rowIdx.getAndIncrement() + ref.getRow(), sheet::getRow, sheet::createRow), ref.getCol())
                .addCellWithValue("22")
                .addCellWithValue("Состав и количество технических средств и персонала, " +
                    "находящихся на месте и направленных к месту отказа, повреждения " +
                    "(указать откуда вышли бригады и расстояние до места аварии/отказа/инцидента, " +
                    "время выхода и прибытия на место)")
                .addCellWithValue(Option.of(incident.getSostav()).orElse(HYPHEN));
            //23
            Row.of(objectNotNull(rowIdx.getAndIncrement() + ref.getRow(), sheet::getRow, sheet::createRow), ref.getCol())
                .addCellWithValue("23")
                .addCellWithValue("Дата и время извещения филиала ОАО «Связьтранснефть» об отказе, " +
                    "повреждении линейной запорной арматуры")
                .addCellWithValue(Option.of(incident.getSvyazTNT()).orElse(HYPHEN));
        }
        return workbook;
    }

    public static <Re extends SourceFututeHSSFWorkBookReport> HSSFWorkbook fromOptionIncidentToReportPlan(Re report, Option<Incident> incidentOption) {
        HSSFWorkbook workbook = report.getHSSFWorkbookFuture().get();
        CellReference ref = new CellReference(workbook.getName(report.getDataBagCellName()).getRefersToFormula());
        HSSFSheet sheet = workbook.getSheet(ref.getSheetName());
        AtomicInteger rowIdx = new AtomicInteger(0);
        if (incidentOption.isDefined()) {
            Incident incident = incidentOption.get();
            List<Working> workingList = incident.getWorking();
            if (workingList == null || workingList.isEmpty()) {
                Row.of(objectNotNull(rowIdx.getAndIncrement() + ref.getRow(), sheet::getRow, sheet::createRow), ref.getCol())
                    .addCellWithValue(SPACE)
                    .addCellWithValue("нет данных")
                    .addCellWithValue(SPACE)
                    .addCellWithValue(SPACE)
                    .addCellWithValue(SPACE);
            } else {
                ofAll(workingList).forEach(working ->
                    Row.of(objectNotNull(rowIdx.getAndIncrement() + ref.getRow(), sheet::getRow, sheet::createRow), ref.getCol())
                        //1
                        .addCellWithValue(String.format("%d", rowIdx.get()))
                        //2
                        .addCellWithValue(Option.of(working.getFullDescription()).orElse(HYPHEN))
                        //3
                        .addCellWithValue(Option.of(working.getControlDate())
                            .map(v -> String.format("%1$td.%1$tm.%1$tY %1$TR", v))
                            .orElse(HYPHEN))
                        //4
                        .addCellWithValue(Option.of(working.getIspolnitel()).orElse(HYPHEN))
                        //5 empty
                        .addCellWithValue(SPACE)
                        //6
                        .addCellWithValue(Option.of(incident.getOst()).orElse(SPACE))
                );
            }
        }
        return workbook;
    }

    public static <Re extends SourceFututeHSSFWorkBookReport> HSSFWorkbook fromIncidentListToReportIncidentByStatus(Re report, List<Incident> incidents) {
        HSSFWorkbook workbook = report.getHSSFWorkbookFuture().get();
        CellReference ref = new CellReference(workbook.getName(report.getDataBagCellName()).getRefersToFormula());
        HSSFSheet sheet = workbook.getSheet(ref.getSheetName());
        AtomicInteger rowIdx = new AtomicInteger(0);
        if (incidents == null || incidents.isEmpty()) {
            Row.of(objectNotNull(rowIdx.getAndIncrement() + ref.getRow(), sheet::getRow, sheet::createRow), ref.getCol())
                .addCellWithValue(SPACE) //1
                .addCellWithValue(SPACE) //2
                .addCellWithValue(NO_DATA) //3
                .addCellWithValue(SPACE) //4
                .addCellWithValue(SPACE) //5
                .addCellWithValue(SPACE) //6
                .addCellWithValue(SPACE) //7
                .addCellWithValue(SPACE) //8
                .addCellWithValue(SPACE) //9
                .addCellWithValue(SPACE) //10
                .addCellWithValue(SPACE) //11
                .addCellWithValue(SPACE) //12
                .addCellWithValue(SPACE) //13
                //14
                .addCellWithValue(ZonedDateTime.now(ZoneId.of("Europe/Moscow")).format(DateTimeFormatter.ofPattern("dd.MM.uuuu", RUSSIAN)))
                .addCellWithValue(SPACE) //15
            ;

        } else {
            Map<String, List<Incident>> incidentByStatus = new HashMap<>();
            for (Incident incident : incidents) {
                String status = incident.getIncStatus();
                if (status == null || status.isEmpty()) {
                    status = "Без статуса";
                }
                List<Incident> list = incidentByStatus.get(status);
                if (list == null) {
                    list = new ArrayList<>();
                }
                list.add(incident);
                incidentByStatus.put(status, list);
            }
            incidentByStatus.forEach((status, list) ->
                ofAll(list).forEach(incident ->
                    Row.of(objectNotNull(rowIdx.getAndIncrement() + ref.getRow(), sheet::getRow, sheet::createRow), ref.getCol())
                        .addCellWithValue(SPACE) //1
                        //2
                        .addCellWithValue(Option.of(incident.getCode()).orElse(HYPHEN))
                        //3
                        .addCellWithValue(Option.of(incident.getDescription()).orElse(HYPHEN))
                        .addCellWithValue(Option.of(incident.getIncReason()).orElse(HYPHEN)) //4
                        //5
                        .addCellWithValue(Option.of(incident.getEventTimeDate())
                            .map(v -> String.format(RUSSIAN, "%1$td.%1$tm.%1$tY %1$TR", v))
                            .orElse(HYPHEN))
                        //6
                        .addCellWithValue(Option.of(incident.getDatePerformance())
                            .map(v -> String.format(RUSSIAN, "%1$td.%1$tm.%1$tY %1$TR", v))
                            .orElse(HYPHEN)
                        )
                        //7
                        .addCellWithValue(Option.of(incident.getIncType())
                            .map(Lookup::getValue)
                            .orElse(HYPHEN))
                        //8
                        .addCellWithValue(Option.of(incident.getUrgencyStr()).orElse(HYPHEN))
                        //9
                        .addCellWithValue(Option.of(incident.getKlassIntident())
                            .map(Lookup::getValue)
                            .orElse(HYPHEN))
                        //10
                        .addCellWithValue(Option.of(incident.getKlassIntident())
                            .map(Lookup::getValue)
                            .orElse(HYPHEN))
                        //11
                        .addCellWithValue(Option.of(incident.getOst()).orElse(HYPHEN))
                        //12
                        .addCellWithValue(Option.of(incident.getRnu()).orElse(HYPHEN))
                        //13
                        .addCellWithValue(Option.of(incident.getTu()).orElse(HYPHEN))
                        //14
                        .addCellWithValue(ZonedDateTime.now(ZoneId.of("Europe/Moscow"))
                            .format(DateTimeFormatter.ofPattern("dd.MM.uuuu", RUSSIAN)))
                        //15
                        .addCellWithValue(status)
                )
            );
        }
        return workbook;
    }

    public static <Re extends SourceFututeHSSFWorkBookReport> HSSFWorkbook fromIncidentListToReportServiceRequestsByIncident(Re report, List<Incident> incidents) {
        HSSFWorkbook workbook = report.getHSSFWorkbookFuture().get();
        CellReference ref = new CellReference(workbook.getName(report.getDataBagCellName()).getRefersToFormula());
        HSSFSheet sheet = workbook.getSheet(ref.getSheetName());
        AtomicInteger rowIdx = new AtomicInteger(0);
        if (incidents == null || incidents.isEmpty()) {
            Row.of(objectNotNull(rowIdx.getAndIncrement() + ref.getRow(), sheet::getRow, sheet::createRow), ref.getCol())
                .addCellWithValue(SPACE) //1
                .addCellWithValue(NO_DATA) //2
                .addCellWithValue(SPACE) //3
                .addCellWithValue(SPACE) //4
                .addCellWithValue(SPACE) //5
                .addCellWithValue(SPACE) //6
                .addCellWithValue(SPACE) //7
                .addCellWithValue(SPACE) //8
                //9
                .addCellWithValue(ZonedDateTime.now(ZoneId.of("Europe/Moscow"))
                    .format(DateTimeFormatter.ofPattern("dd.MM.uuuu", RUSSIAN)))
                .addCellWithValue(SPACE);//10
        } else {
            ofAll(incidents).forEach(incident ->
                ofAll(incident.getInicialIncidents())
                    .filter(Objects::nonNull)
                    .forEach(serviceRequest ->
                        Row.of(objectNotNull(rowIdx.getAndIncrement() + ref.getRow(), sheet::getRow, sheet::createRow), ref.getCol())
                            //1
                            .addCellWithValue(Option.of(serviceRequest.getCode()).orElse(HYPHEN))
                            //2
                            .addCellWithValue(Option.of(serviceRequest.getDescription()).orElse(HYPHEN))
                            //3
                            .addCellWithValue(
                                Option.of(serviceRequest.getUrgency())
                                    .map(Urgency::getDescription)
                                    .orElse(HYPHEN))
                            //4
                            .addCellWithValue(
                                Option.of(serviceRequest.getImpactSelect())
                                    .map(ImpactSelect::getDescription)
                                    .orElse(HYPHEN))
                            //5
                            .addCellWithValue(
                                Option.of(serviceRequest.getSouceIncome())
                                    .map(SouceIncome::getDescription)
                                    .orElse(HYPHEN))
                            //6
                            .addCellWithValue(Option.of(serviceRequest.getOst()).orElse(HYPHEN))
                            //7
                            .addCellWithValue(Option.of(serviceRequest.getTu()).orElse(HYPHEN))
                            //8
                            .addCellWithValue(Option.of(serviceRequest.getLinePart_NPS()).orElse(HYPHEN))
                            //9
                            .addCellWithValue(ZonedDateTime.now(ZoneId.of("Europe/Moscow"))
                                .format(DateTimeFormatter.ofPattern("dd.MM.uuuu", RUSSIAN)))
                            //10
                            .addCellWithValue(incident.getCode())
                    )
            );
        }
        return workbook;
    }

    public static <Re extends SourceFututeHSSFWorkBookReport> HSSFWorkbook fromDirectionListToReportExpiredDirections(Re report, List<Direction> directions) {
        HSSFWorkbook workbook = report.getHSSFWorkbookFuture().get();
        CellReference ref = new CellReference(workbook.getName(report.getDataBagCellName()).getRefersToFormula());
        HSSFSheet sheet = workbook.getSheet(ref.getSheetName());
        AtomicInteger rowIdx = new AtomicInteger(0);
        List<Direction> expiredDirections = ofAll(directions)
            .filter(direction ->
                direction.getRemind().before(new Date()) &&
                    !direction.getStatusCode().equals("realized"))
            .toJavaList();
        if (expiredDirections == null || expiredDirections.isEmpty()) {
            final int EMPTY_LIST = 0;
            Row.of(objectNotNull(rowIdx.getAndIncrement() + ref.getRow(), sheet::getRow, sheet::createRow), ref.getCol())
                .addCellWithValue(SPACE) //1
                .addCellWithValue("Нет поручений") //2
                .addCellWithValue(SPACE) //3
                .addCellWithValue(SPACE) //4
                .addCellWithValue(SPACE) //5
                .addCellWithValue(SPACE) //6
                .addCellWithValue(SPACE) //7
                .addCellWithValue(SPACE) //8
                //9
                .addCellWithValue(ZonedDateTime.now(ZoneId.of("Europe/Moscow"))
                    .format(DateTimeFormatter.ofPattern("dd.MM.uuuu", RUSSIAN)))
                //10
                .addCellWithValue(String.format("%d", EMPTY_LIST));
        } else {
            ofAll(expiredDirections).forEach(direction ->
                Row.of(objectNotNull(rowIdx.getAndIncrement() + ref.getRow(), sheet::getRow, sheet::createRow), ref.getCol())
                    //1
                    .addCellWithValue(String.format("%d", rowIdx.get()))
                    //2
                    .addCellWithValue(Option.of(direction.getDescription()).orElse(NO))
                    //3
                    .addCellWithValue(Option.of(direction.getRegTime()).orElse(NO))
                    //4
                    .addCellWithValue(Option.of(direction.getRemind())
                        .map(v -> String.format(RUSSIAN, "%1$td.%1$tm.%1$tY %1$TR", v))
                        .orElse(NO))
                    //5
                    .addCellWithValue(Option.of(direction.getIspolnitel()).orElse(NO))
                    //6
                    .addCellWithValue(Option.of(direction.getNumber()).orElse(NO))
                    //7
                    .addCellWithValue(Option.of(direction.getName()).orElse(NO))
                    //8
                    .addCellWithValue(Option.of(direction.getDocumentType()).orElse(NO))
                    //9
                    .addCellWithValue(ZonedDateTime.now(ZoneId.of("Europe/Moscow"))
                        .format(DateTimeFormatter.ofPattern("dd.MM.uuuu", RUSSIAN)))
                    //10
                    .addCellWithValue(String.format("%d", expiredDirections.size()))
            );
        }
        return workbook;
    }

    public static String joiningNewLine(String acc, String ps) {
        return acc.concat("\n").concat(ps);
    }

    public static String joiningWithSemicolonAndNewLine(String acc, String ps) {
        return acc.concat(";\n").concat(ps);
    }

    public static String joiningWithSpace(String acc, String ps) {
        return acc.concat(" ").concat(ps);
    }
}
