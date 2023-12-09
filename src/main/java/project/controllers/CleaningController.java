package project.controllers;

import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import project.models.*;
import project.repositories.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;

import java.io.IOException;

import java.util.*;

import static com.github.demidko.aot.WordformMeaning.lookupForMeanings;

@Controller
public class CleaningController {

    @Autowired
    private UserRepository userRepository;
    @GetMapping("/")
    public String home() {
        return "home";
    }
    @GetMapping("/loadRep")
    public String showBriefs(Authentication authentication, Model model){
        User user1 = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Users user = userRepository.findByLogin(user1.getUsername());
//        model.addAttribute("briefs", briefRepository.findAllByClient(user));
        return "loadReport";
    }

    private List<SearchQuery> searchQueries = new ArrayList<SearchQuery>();
    private List<Metric> metrics = new ArrayList<Metric>();
    private List<Metric> filteredMetrics = new ArrayList<Metric>();

    private String addSystem;

    private String filters = " ";

    private Clean clean;

    @PostMapping("/loadReport")
    public String loadReport (Model model, @RequestParam("fileInput") MultipartFile[] reports, @RequestParam("name") String name, @RequestParam("system") String system) throws IOException {
        searchQueries.clear();
        metrics.clear();
        XSSFWorkbook workbookNew = new XSSFWorkbook(reports[0].getInputStream());
//        XSSFSheet worksheetNew = workbookNew.getSheetAt(0);

        // создание самого excel файла в памяти
//        XSSFWorkbook workbookPrev = new XSSFWorkbook();
//        // создание листа с названием "Просто лист"
//        XSSFSheet worksheetPrev = workbookPrev.createSheet("Лист 1");
//
        String UPLOAD_DIRECTORY = System.getProperty("user.dir") + "/src/main/resources/static/tables/table.xlsx";
//        FileOutputStream out = new FileOutputStream(new File(UPLOAD_DIRECTORY));

//        for (int i = 0; i < worksheetNew.getPhysicalNumberOfRows(); i++) {
//            XSSFRow rowPrev = worksheetPrev.createRow(worksheetNew.getPhysicalNumberOfRows()+1);
//            XSSFRow rowNew = worksheetNew.getRow(i);
//            for( int k = 0; k < rowNew.getPhysicalNumberOfCells(); k++) {
//               rowPrev.createCell(i).setCellValue(rowNew.getCell(i).toString());
//                Cell cell = rowPrev.createCell(i);
//                cell.setCellValue(rowNew.getCell(i));
//            }
//        }

        if (system.contains("Яндекс")) {
            Clean clean1 = new Clean();
            User user1 = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            Users user = userRepository.findByLogin(user1.getUsername());
            clean1.setUser(user);
            clean1.setProject(name);
            clean1.setAdvertisingSystem("Яднекс");
            if (checkYandexReport(workbookNew)) {
                String code = workbookNew.getSheetAt(0).getRow(4).getCell(10).toString().substring(8,10);
                for (int i = 4; i < workbookNew.getSheetAt(0).getPhysicalNumberOfRows(); i++) {
                    XSSFRow row = workbookNew.getSheetAt(0).getRow(i);
                    if (row.getCell(7).toString().equals("фраза")) {
                        SearchQuery searchQuery = new SearchQuery();
                        searchQuery.setText(row.getCell(0).toString());
                        searchQuery.setCampaign(row.getCell(1).toString());
                        searchQuery.setAddGroup(row.getCell(3).toString());
                        searchQuery.setKeyword(row.getCell(5).toString());
                        searchQueries.add(searchQuery);
                        Metric metric = new Metric();
                        metric.setClicks(Integer.parseInt(row.getCell(9).toString().substring(0,(row.getCell(9).toString().length()-2))));
                        metric.setShows(Integer.parseInt(row.getCell(8).toString().substring(0,(row.getCell(8).toString().length()-2))));
                        String conversions = row.getCell(11).toString();
                        if (!conversions.equals("-")) metric.setConversions(Integer.parseInt(conversions.substring(0,(row.getCell(11).toString().length()-2))));
                        else metric.setConversions(0);
                        metric.setConsumption(Float.valueOf(row.getCell(10).toString()));
                        metric.setCurrencyCode(code);
                        metric.setSearchQuery(searchQuery);
                        metrics.add(metric);
                    }
                }
                model.addAttribute("data1", createPieSrting(metrics, 1));
                model.addAttribute("data2", createPieSrting(metrics, 2));
                model.addAttribute("metrics", metrics);
                return "analysis";
            }
            else {
                model.addAttribute("error", "ошибка");
                return "loadReport";
            }
        } else {
            if (checkGoogleReport(workbookNew))  return "loadReport";
            else return "home";
        }
    }
    private boolean checkYandexReport (XSSFWorkbook workbook) {
        try {
            XSSFSheet worksheet = workbook.getSheetAt(0);
            XSSFRow row = worksheet.getRow(4);
            if (row.getCell(0).toString().equals("Поисковый запрос") & row.getCell(1).toString().equals("Кампания") & row.getCell(3).toString().equals("Группа") & row.getCell(5).toString().equals("Условие показа") & row.getCell(7).toString().equals("Тип условия показа") & row.getCell(8).toString().equals("Показы") & row.getCell(9).toString().equals("Клики") & row.getCell(10).toString().contains("Расход") & row.getCell(11).toString().equals("Конверсии"))
                return true;
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    private boolean checkGoogleReport (XSSFWorkbook workbook) {
        return true;
    }

    @PostMapping("/filter")
    public String filter (Model model, @RequestParam("campaign") String campaign, @RequestParam("group") String group, @RequestParam("query") String query, @RequestParam("Showsselect") String showsselect,
                          @RequestParam("Shows") String shows, @RequestParam("Clicksselect") String clicksselect, @RequestParam("Clicks") String clicks, @RequestParam("CTRselect") String ctrselect, @RequestParam("CTR") String ctr,
                          @RequestParam("СostClickselect") String costClickSelect, @RequestParam("СostClick") String costClick, @RequestParam("Convselect") String convSelect, @RequestParam("Conv") String conv,
                          @RequestParam("PerConvselect") String perConvSelect, @RequestParam("PerConv") String perConv, @RequestParam("СostConvselect") String costConvSelect,
                          @RequestParam("СostConv") String costConv, @RequestParam("Сonsselect") String consSelect, @RequestParam("Сons") String cons) throws JsonProcessingException {
        filteredMetrics.clear();
        for (Metric metric:metrics) {
            filteredMetrics.add(metric);
        }
        Iterator<Metric> iterator = filteredMetrics.iterator();
        if (!campaign.equals("")) {
            iterator = filteredMetrics.iterator();
            while (iterator.hasNext()) {
                Metric metric = iterator.next();
                if (!metric.getSearchQuery().getCampaign().contains(campaign)) iterator.remove();
            }
            filters+=("Кампания содержит: " + campaign + "; ");
        }
        if (!group.equals("")) {
            iterator = filteredMetrics.iterator();
            while (iterator.hasNext()) {
                Metric metric = iterator.next();
                if (!metric.getSearchQuery().getAddGroup().contains(group)) iterator.remove();
            }
            filters+=("Группа содержит: " + group + "; ");
        }
        if (!query.equals("")) {
            iterator = filteredMetrics.iterator();
            while (iterator.hasNext()) {
                Metric metric = iterator.next();
                if (!metric.getSearchQuery().getText().contains(query)) iterator.remove();
            }
            filters+=("Запрос содержит: " + query + "; ");
        }
        if (!shows.equals("")) {
            if (showsselect.equals("Больше"))
            {
                iterator = filteredMetrics.iterator();
                while (iterator.hasNext()) {
                    Metric metric = iterator.next();
                   if (!(metric.getShows()>Integer.parseInt(shows))) iterator.remove();
                }
                filters+=("Показов > " + shows.toString() + "; ");
            } else if (showsselect.equals("Меньше"))
            {
                iterator = filteredMetrics.iterator();
                while (iterator.hasNext()) {
                    Metric metric = iterator.next();
                    if (!(metric.getShows()< Integer.parseInt(shows))) iterator.remove();
                }
                filters+=("Показов < " + shows.toString() + "; ");
            } else {
                iterator = filteredMetrics.iterator();
                while (iterator.hasNext()) {
                    Metric metric = iterator.next();
                    if (metric.getShows()!=Integer.parseInt(shows)) iterator.remove();
                }
                filters+=("Показов " + shows.toString() + "; ");
            }
        }
        if (!clicks.equals("")) {
            if (clicksselect.equals("Больше"))
            {
                iterator = filteredMetrics.iterator();
                while (iterator.hasNext()) {
                    Metric metric = iterator.next();
                    if (!(metric.getClicks()>Integer.parseInt(clicks))) iterator.remove();
                }
                filters+=("Кликов > " + clicks.toString() + "; ");
            } else if (clicksselect.equals("Меньше"))
            {
                iterator = filteredMetrics.iterator();
                while (iterator.hasNext()) {
                    Metric metric = iterator.next();
                    if (!(metric.getClicks()< Integer.parseInt(clicks))) iterator.remove();
                }
                filters+=("Кликов < " + clicks.toString() + "; ");
            } else {
                iterator = filteredMetrics.iterator();
                while (iterator.hasNext()) {
                    Metric metric = iterator.next();
                    if (metric.getClicks()!=Integer.parseInt(clicks)) iterator.remove();
                }
            }
            filters+=("Кликов " + clicks.toString() + "; ");
        }
        if (!ctr.equals("")) {
            if (ctrselect.equals("Больше"))
            {
                iterator = filteredMetrics.iterator();
                while (iterator.hasNext()) {
                    Metric metric = iterator.next();
                    if (metric.getShows()!=0) {
                        if (!((metric.getClicks() * 1.0 / metric.getShows()) > Double.parseDouble(ctr)))
                            iterator.remove();
                    } else iterator.remove();
                }
                filters+=("CTR > " + ctr.toString() + "; ");
            } else if (ctrselect.equals("Меньше"))
            {
                iterator = filteredMetrics.iterator();
                while (iterator.hasNext()) {
                    Metric metric = iterator.next();
                    if (metric.getShows()!=0) {
                        if (!((metric.getClicks() * 1.0 / metric.getShows()) < Double.parseDouble(ctr)))
                            iterator.remove();
                    } else iterator.remove();
                }
                filters+=("CTR < " + ctr.toString() + "; ");
            } else {
                iterator = filteredMetrics.iterator();
                while (iterator.hasNext()) {
                    Metric metric = iterator.next();
                    if (metric.getShows()!=0) {
                        if ((metric.getClicks() * 1.0 / metric.getShows()) != Double.parseDouble(ctr))
                            iterator.remove();
                    } else iterator.remove();
                }
                filters+=("CTR " + ctr.toString() + "; ");
            }
        }
        if (!costClick.equals("")) {
            if (costClickSelect.equals("Больше"))
            {
                iterator = filteredMetrics.iterator();
                while (iterator.hasNext()) {
                    Metric metric = iterator.next();
                    if (metric.getClicks()!=0) {
                        if (!((metric.getConsumption() * 1.0 / metric.getClicks()) > Double.parseDouble(costClick)))
                            iterator.remove();
                    } else iterator.remove();
                }
                filters+=("Цена клика > " + costClick.toString() + "; ");
            } else if (costClickSelect.equals("Меньше"))
            {
                iterator = filteredMetrics.iterator();
                while (iterator.hasNext()) {
                    Metric metric = iterator.next();
                    if (metric.getClicks()!=0) {
                        if (!((metric.getConsumption() * 1.0 / metric.getClicks()) < Double.parseDouble(costClick)))
                            iterator.remove();
                    } else iterator.remove();}
                filters+=("Цена клика < " + costClick.toString() + "; ");
            } else {
                iterator = filteredMetrics.iterator();
                while (iterator.hasNext()) {
                    Metric metric = iterator.next();
                    if (metric.getClicks()!=0) {
                        if ((metric.getConsumption() * 1.0 / metric.getClicks())!=Double.parseDouble(costClick))
                            iterator.remove();
                    } else iterator.remove();}
                filters+=("Цена клика " + costClick.toString() + "; ");
            }
        }
        if (!conv.equals("")) {
            if (convSelect.equals("Больше"))
            {
                iterator = filteredMetrics.iterator();
                while (iterator.hasNext()) {
                    Metric metric = iterator.next();
                    if (!(metric.getConversions()>Integer.parseInt(conv))) iterator.remove();
                }
                filters+=("Конверсий > " + conv.toString() + "; ");
            } else if (convSelect.equals("Меньше"))
            {
                iterator = filteredMetrics.iterator();
                while (iterator.hasNext()) {
                    Metric metric = iterator.next();
                    if (!(metric.getConversions()< Integer.parseInt(conv))) iterator.remove();
                }
                filters+=("Конверсий < " + conv.toString() + "; ");
            } else {
                iterator = filteredMetrics.iterator();
                while (iterator.hasNext()) {
                    Metric metric = iterator.next();
                    if (metric.getConversions()!=Integer.parseInt(conv)) iterator.remove();
                }
                filters+=("Конверсий " + conv.toString() + "; ");
            }
        }
        if (!perConv.equals("")) {
            if (perConvSelect.equals("Больше"))
            {
                iterator = filteredMetrics.iterator();
                while (iterator.hasNext()) {
                    Metric metric = iterator.next();
                    if (metric.getClicks()!=0) {
                        if (!((metric.getConversions() * 1.0 / metric.getClicks()) > Double.parseDouble(perConv)))
                            iterator.remove();
                    } else iterator.remove();
                }
                filters+=("%Конверсий > " + perConv.toString() + "; ");
            } else if (perConvSelect.equals("Меньше"))
            {
                iterator = filteredMetrics.iterator();
                while (iterator.hasNext()) {
                    Metric metric = iterator.next();
                    if (metric.getClicks()!=0) {
                        if (!((metric.getConversions() * 1.0 / metric.getClicks()) < Double.parseDouble(perConv)))
                            iterator.remove();
                    } else iterator.remove();}
                filters+=("%Конверсий < " + perConv.toString() + "; ");
            } else {
                iterator = filteredMetrics.iterator();
                while (iterator.hasNext()) {
                    Metric metric = iterator.next();
                    if (metric.getClicks()!=0) {
                        if ((metric.getConversions() * 1.0 / metric.getClicks())!=Double.parseDouble(perConv))
                            iterator.remove();
                    } else iterator.remove();}
                filters+=("%Конверсий " + perConv.toString() + "; ");
            }
        }
        if (!costConv.equals("")) {
            if (costConvSelect.equals("Больше"))
            {
                iterator = filteredMetrics.iterator();
                while (iterator.hasNext()) {
                    Metric metric = iterator.next();
                    if (metric.getConversions()!=0) {
                        if (!((metric.getConsumption() * 1.0 / metric.getConversions()) > Double.parseDouble(costConv)))
                            iterator.remove();
                    } else iterator.remove();
                }
                filters+=("Цена цели > " + costConv.toString() + "; ");
            } else if (costConvSelect.equals("Меньше"))
            {
                iterator = filteredMetrics.iterator();
                while (iterator.hasNext()) {
                    Metric metric = iterator.next();
                    if (metric.getConversions()!=0) {
                        if (!((metric.getConsumption() * 1.0 / metric.getConversions()) < Double.parseDouble(costConv)))
                            iterator.remove();
                    } else iterator.remove();}
                filters+=("Цена цели < " + costConv.toString() + "; ");
            } else {
                iterator = filteredMetrics.iterator();
                while (iterator.hasNext()) {
                    Metric metric = iterator.next();
                    if (metric.getConversions()!=0) {
                        if ((metric.getConsumption() * 1.0 / metric.getConversions())!=Double.parseDouble(costConv))
                            iterator.remove();
                    } else iterator.remove();}
                filters+=("Цена цели " + costConv.toString() + "; ");
            }
        }
        if (!cons.equals("")) {
            if (consSelect.equals("Больше"))
            {
                iterator = filteredMetrics.iterator();
                while (iterator.hasNext()) {
                    Metric metric = iterator.next();
                    if (!(metric.getConsumption()>Integer.parseInt(cons))) iterator.remove();
                }
                filters+=("Расход > " + cons.toString() + "; ");
            } else if (consSelect.equals("Меньше"))
            {
                iterator = filteredMetrics.iterator();
                while (iterator.hasNext()) {
                    Metric metric = iterator.next();
                    if (!(metric.getConsumption()< Integer.parseInt(cons))) iterator.remove();
                }
                filters+=("Расход < " + cons.toString() + "; ");
            } else {
                iterator = filteredMetrics.iterator();
                while (iterator.hasNext()) {
                    Metric metric = iterator.next();
                    if (metric.getConsumption()!=Integer.parseInt(cons)) iterator.remove();
                }
                filters+=("Расход " + cons.toString() + "; ");
            }
        }
        if (filters.equals("")) filters = "none";
        if(!filteredMetrics.isEmpty()) {
            model.addAttribute("data1", createPieSrting(filteredMetrics, 1));
            model.addAttribute("data2", createPieSrting(filteredMetrics, 2));
        } else {
            model.addAttribute("data1", createPieSrting(metrics, 1));
            model.addAttribute("data2", createPieSrting(metrics, 2));
        }
        model.addAttribute("filters", filters);
        model.addAttribute("metrics", filteredMetrics);
    return "analysis";
    }
    @PostMapping("/forecast")
    public String forecast (Model model) throws IOException {
        List<SearchQuery> badSearchQueries = new ArrayList<SearchQuery>();
        List<NegPhrase> negPhrases = new ArrayList<>();
        List<String> negPhrasesTexts = new ArrayList<String>();
        List<String> articleList = new ArrayList<String>();
        articleList(articleList);
        for (SearchQuery searchQuery: searchQueries) {
            String[] text = searchQuery.getText().split("\\s+");
            String[] key = searchQuery.getKeyword().split("\\s+");
            List<String> textList = Arrays.asList(lemmaList(text));
            List<String> keyList = Arrays.asList(lemmaList(key));
            negPhrasesTexts = new ArrayList<>(textList);
            negPhrasesTexts.removeAll(keyList);
            negPhrasesTexts.removeAll(articleList);
            if (!negPhrasesTexts.isEmpty()) {
                for (String negPhrasesText:negPhrasesTexts) {
                    System.out.println(negPhrasesText);
                    NegPhrase negPhrase = new NegPhrase();
                    negPhrase.setText(negPhrasesText);
                    negPhrase.setStatus("new");
                    negPhrase.setSearchQuery(searchQuery);
                    negPhrases.add(negPhrase);
                }
            }
        }
        model.addAttribute("negPhrases", negPhrases);
        return "forecast";
    }
    @PostMapping("/save_xml")
    public String saveXML (Model model) throws IOException {
        XSSFWorkbook workbook = new XSSFWorkbook();
        workbook.createSheet("Отчёт");
        XSSFSheet worksheet = workbook.getSheetAt(0);
        if (!filteredMetrics.isEmpty()) addDataToReport(filteredMetrics, worksheet);
        else addDataToReport(metrics, worksheet);

        if(!filteredMetrics.isEmpty()) {
            model.addAttribute("data1", createPieSrting(filteredMetrics, 1));
            model.addAttribute("data2", createPieSrting(filteredMetrics, 2));
            model.addAttribute("filters", filters);
            model.addAttribute("metrics", filteredMetrics);
        } else {
            model.addAttribute("data1", createPieSrting(metrics, 1));
            model.addAttribute("data2", createPieSrting(metrics, 2));
            model.addAttribute("metrics", metrics);
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        byte[] workbookBytes = outputStream.toByteArray();
        String workbookBase64 = Base64.getEncoder().encodeToString(workbookBytes);
        model.addAttribute("reportXSLX", workbookBase64);

        return "analysis";
    }

    private void addDataToReport (List<Metric> metrics, XSSFSheet worksheet) {
        XSSFRow headerRow = worksheet.createRow(0);
        headerRow.createCell(0).setCellValue("Кампания");
        headerRow.createCell(1).setCellValue("Группа");
        headerRow.createCell(2).setCellValue("Поисковый запрос");
        headerRow.createCell(3).setCellValue("Показы");
        headerRow.createCell(4).setCellValue("Клики");
        headerRow.createCell(5).setCellValue("CTR");
        headerRow.createCell(6).setCellValue("Цена клика");
        headerRow.createCell(7).setCellValue("Конверсии");
        headerRow.createCell(8).setCellValue("%Конверсии");
        headerRow.createCell(9).setCellValue("Цена цели");
        headerRow.createCell(10).setCellValue("Расход");
        for (int i = 0; i< metrics.size();i++) {
            Metric metric = metrics.get(i);
            int shows = metric.getShows(), clicks = metric.getClicks(), convs = metric.getConversions();
            double cons = metric.getConsumption();
            XSSFRow headerRow1 = worksheet.createRow(i+1);
            headerRow1.createCell(0).setCellValue(metric.getSearchQuery().getCampaign());
            headerRow1.createCell(1).setCellValue(metric.getSearchQuery().getAddGroup());
            headerRow1.createCell(2).setCellValue(metric.getSearchQuery().getText());
            headerRow1.createCell(3).setCellValue(shows);
            headerRow1.createCell(4).setCellValue(clicks);
            if (shows!=0)
                headerRow1.createCell(5).setCellValue(String.format("%.2f",(1.0*clicks/shows)));
            else headerRow1.createCell(5).setCellValue("-");
            if (clicks!=0)
                headerRow1.createCell(6).setCellValue(String.format("%.2f",(cons/clicks)));
            else headerRow1.createCell(6).setCellValue("-");
            headerRow1.createCell(7).setCellValue(convs);
            if (clicks!=0)
                headerRow1.createCell(8).setCellValue(String.format("%.2f",(1.0*convs/clicks)));
            else headerRow1.createCell(8).setCellValue("-");
            if (convs!=0)
                headerRow1.createCell(9).setCellValue(String.format("%.2f",(cons/clicks)));
            else headerRow1.createCell(9).setCellValue("-");
            headerRow1.createCell(10).setCellValue(metric.getConsumption());
        }
    }
    private String createPieSrting (List<Metric> metrics, int chart) throws JsonProcessingException {
        Integer shows = 0, clicks = 0, convs = 0;
        for (Metric metric1:metrics) {
            shows+=metric1.getShows();
            clicks+=metric1.getClicks();
            convs+=metric1.getConversions();

        }
        ObjectMapper objectMapper = new ObjectMapper();
        if (chart==1) {
            List<Integer> data1 = Arrays.asList(shows,clicks);
            return objectMapper.writeValueAsString(data1);
        }
        else {
            List<Integer> data2 = Arrays.asList(clicks, convs);
            return objectMapper.writeValueAsString(data2);
        }
    }

    private void articleList (List<String> articleList) {
        articleList.add("в");
        articleList.add("за");
        articleList.add("на");
        articleList.add("до");
        articleList.add("к");
        articleList.add("под");
        articleList.add("через");
        articleList.add("для");
        articleList.add("по");
        articleList.add("от");
        articleList.add("у");
    }

    private String[] lemmaList (String [] list) {
        String[] lemmaList = new String[list.length];
        for (int i = 0; i<list.length; i++) {
            try {
                var meanings = lookupForMeanings(list[i]);
                lemmaList[i] = meanings.get(0).getLemma().toString();
            }
            catch (Exception e) {
                lemmaList[i] = list[i];
            }

        }
        return lemmaList;
    }

}
