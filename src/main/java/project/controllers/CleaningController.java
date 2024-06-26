package project.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
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
import project.repositories.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;

import java.util.*;
import java.util.stream.Collectors;

import static com.github.demidko.aot.WordformMeaning.lookupForMeanings;

@Controller
public class CleaningController {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private MetricRepository metricRepository;
    @Autowired
    private CleanRepository cleanRepository;
    @Autowired
    private NegPhraseRepository negPhraseRepository;
    @Autowired
    private SearchQueryRepository searchQueryRepository;
    @GetMapping("/")
    public String home() {
        return "home";
    }
    @GetMapping("/loadRep")
    public String showBriefs(Authentication authentication, Model model){
        User user1 = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Users user = userRepository.findByLogin(user1.getUsername());
        return "loadReport";
    }

    private List<SearchQuery> searchQueries = new ArrayList<SearchQuery>();
    private List<Metric> metrics = new ArrayList<Metric>();
    private List<Metric> filteredMetrics = new ArrayList<Metric>();
    private List<Metric> selectedMetrics = new ArrayList<Metric>();

    private List<NegPhrase> negPhrases = new ArrayList<NegPhrase>();

    private List<NegPhrase> selectedNegPhrases = new ArrayList<NegPhrase>();
    private String addSystem;

    private String filters = " ";

    private Clean clean = new Clean();

    @PostMapping("/loadReport")
    public String loadReport (Model model, @RequestParam("fileInput") MultipartFile[] reports, @RequestParam("name") String name, @RequestParam("system") String system) throws IOException {
        searchQueries.clear();
        filteredMetrics.clear();
        selectedMetrics.clear();
        metrics.clear();
        User user1 = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Users user = userRepository.findByLogin(user1.getUsername());
        clean.setUser(user);
        clean.setProject(name);
        clean.setAdvertisingSystem(system);
    try {
        if (system.contains("Яндекс")) {
            addSystem="Яндекс";
            XSSFWorkbook workbookNew = new XSSFWorkbook(reports[0].getInputStream());
            if (checkYandexReport(workbookNew)) {
                String code = workbookNew.getSheetAt(0).getRow(4).getCell(10).toString().substring(8, 10);
                for (int i = 4; i < workbookNew.getSheetAt(0).getPhysicalNumberOfRows(); i++) {
                    XSSFRow row = workbookNew.getSheetAt(0).getRow(i);
                    if (row.getCell(7).toString().equals("фраза")) {
                        SearchQuery searchQuery = new SearchQuery();
                        searchQuery.setText(row.getCell(0).toString());
                        searchQuery.setCampaign(row.getCell(1).toString());
                        searchQuery.setAddGroup(row.getCell(3).toString());
                        searchQuery.setKeyword(row.getCell(5).toString());
                        searchQuery.setId(i);
                        searchQueries.add(searchQuery);
                        Metric metric = new Metric();
                        metric.setClicks(Integer.parseInt(row.getCell(9).toString().substring(0, (row.getCell(9).toString().length() - 2))));
                        metric.setShows(Integer.parseInt(row.getCell(8).toString().substring(0, (row.getCell(8).toString().length() - 2))));
                        String conversions = row.getCell(11).toString();
                        if (!conversions.equals("-"))
                            metric.setConversions(Integer.parseInt(conversions.substring(0, (row.getCell(11).toString().length() - 2))));
                        else metric.setConversions(0);
                        metric.setConsumption(Float.valueOf(row.getCell(10).toString()));
                        metric.setCurrencyCode(code);
                        metric.setSearchQuery(searchQuery);
                        metric.setId(i);
                        metrics.add(metric);
                    }
                }
                model.addAttribute("data1", createPieSrting(metrics, 1));
                model.addAttribute("data2", createPieSrting(metrics, 2));
                model.addAttribute("metrics", metrics);
                return "analysis";
            } else {
                model.addAttribute("error", "ошибка");
                return "loadReport";
            }
        } else {
            addSystem = "Google";
            CSVFormat csvFormat = CSVFormat.EXCEL.withDelimiter('\t');
            try (InputStream inputStream = reports[0].getInputStream();
                 Reader reader = new InputStreamReader(inputStream, "UTF-16");
                 CSVParser csvParser = new CSVParser(reader, csvFormat)) {
                    int rowIndex = 0;
                    for (CSVRecord csvRecrd : csvParser) {
                        if (rowIndex == 2){
                            try {
                                if (!(csvRecrd.get(0).toString().equals("Кампания") & csvRecrd.get(1).toString().equals("Группа объявлений") & csvRecrd.get(2).toString().equals("Ключевые слова для поисковой рекламы") & csvRecrd.get(3).toString().equals("Поисковый запрос") & csvRecrd.get(4).toString().equals("Показы") & csvRecrd.get(5).toString().equals("Kлики") & csvRecrd.get(6).toString().equals("Все конв.") & csvRecrd.get(7).toString().equals("Код валюты") & csvRecrd.get(8).toString().equals("Расходы"))) {
                                    model.addAttribute("error", "ошибка");
                                    return "loadReport";
                                }
                            } catch (Exception e) {
                                model.addAttribute("error", "ошибка");
                                return "loadReport";
                            }
                        }
                        if (rowIndex >= 3) {
                            SearchQuery searchQuery = new SearchQuery();
                            searchQuery.setText(csvRecrd.get(3).toString());
                            searchQuery.setCampaign(csvRecrd.get(0).toString());
                            searchQuery.setAddGroup(csvRecrd.get(1).toString());
                            searchQuery.setKeyword(csvRecrd.get(2).toString());
                            searchQuery.setId(rowIndex);
                            searchQueries.add(searchQuery);
                            Metric metric = new Metric();
                            metric.setClicks(Integer.parseInt(csvRecrd.get(5).toString()));
                            metric.setShows(Integer.parseInt(csvRecrd.get(4).toString()));
                            metric.setConversions(Math.round(Float.valueOf(csvRecrd.get(6).toString().replace(",", "."))));
                            metric.setConsumption(Float.valueOf(csvRecrd.get(8).toString().replace(",", ".")));
                            metric.setCurrencyCode(csvRecrd.get(7));
                            metric.setSearchQuery(searchQuery);
                            metric.setId(rowIndex);
                            metrics.add(metric);

                        }
                        rowIndex++;
                    }
                    model.addAttribute("data1", createPieSrting(metrics, 1));
                    model.addAttribute("data2", createPieSrting(metrics, 2));
                    model.addAttribute("metrics", metrics);
                    return "analysis";
                }
            }
    } catch (Exception e) {
        model.addAttribute("error", "ошибка");
        return "loadReport";
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
        negPhrases.clear();
        List<String> negPhrasesTexts = new ArrayList<String>();
        List<String> articleList = new ArrayList<String>();
        articleList(articleList);
        int index = 0;
        for (SearchQuery searchQuery: searchQueries) {
            String[] text = searchQuery.getText().split("\\s+");
            String[] key = searchQuery.getKeyword().split("\\s+");
            List<String> textList = new ArrayList<>();
            List<String> keyList = new ArrayList<>();
            if(addSystem.equals("Яндекс")) {
                textList = Arrays.asList(lemmaList(text));
                keyList = Arrays.asList(lemmaList(key));
            } else {
                textList = Arrays.asList(text);
                keyList = Arrays.asList(key);
            }
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
                    negPhrase.setId(index);
                    negPhrases.add(negPhrase);
                    index++;
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
    @PostMapping("/addQueries")
    public String addQueries(Model model, @RequestParam("selectedMetricIds") String selectedMetricIds) {
        List<String> metricIds = new ArrayList<>();
        try {
            metricIds = new ObjectMapper().readValue(selectedMetricIds, new TypeReference<List<String>>() {});
            final List<String> metricIdsCopy = metricIds;
                List<Metric> selectedMetrics1 = metrics.stream()
                        .filter(metric -> metricIdsCopy.contains(String.valueOf(metric.getId())))
                        .collect(Collectors.toList());
            for (Metric metric:selectedMetrics1) {
                System.out.println(metric.getSearchQuery().getText());
                selectedMetrics.add(metric);
            }
            Iterator<Metric> iterator = metrics.iterator();
            while (iterator.hasNext()) {
                Metric metric = iterator.next();
                if (selectedMetrics1.contains(metric)) {
                    iterator.remove();
                }
            }
            if (!filteredMetrics.isEmpty()) {
                Iterator<Metric> iterator1 = filteredMetrics.iterator();
                while (iterator1.hasNext()) {
                    Metric metric = iterator1.next();
                    if (selectedMetrics1.contains(metric)) {
                        iterator1.remove();
                    }
                }
                model.addAttribute("data1", createPieSrting(filteredMetrics, 1));
                model.addAttribute("data2", createPieSrting(filteredMetrics, 2));
                model.addAttribute("filters", filters);
                model.addAttribute("metrics", filteredMetrics);
            }
            else {
                model.addAttribute("data1", createPieSrting(metrics, 1));
                model.addAttribute("data2", createPieSrting(metrics, 2));
                model.addAttribute("metrics", metrics);
            }
            return "analysis";
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return "analysis";
        }
    }
    @PostMapping("/addWords")
    public String addWords(Model model, @RequestParam("selectedNegPhraseIds") String selectedNegPhraseIds) {
        List<String> negIds = new ArrayList<>();
        try {
            negIds = new ObjectMapper().readValue(selectedNegPhraseIds, new TypeReference<List<String>>() {});
            final List<String> negIdsCopy = negIds;
            for (String negId: negIdsCopy) {
                System.out.println(negId);
            }
            for (NegPhrase negPhrase: negPhrases) {
                System.out.println(negPhrase.getId().toString());
            }
            List<NegPhrase> selectedNegPhrases1 = negPhrases.stream()
                    .filter(negPhrase -> negIdsCopy.contains(String.valueOf(negPhrase.getId())))
                    .collect(Collectors.toList());
            for (NegPhrase negPhrase :selectedNegPhrases1) {
                selectedNegPhrases.add(negPhrase);
            }
            Iterator<NegPhrase> iterator = negPhrases.iterator();
            while (iterator.hasNext()) {
                NegPhrase negPhrase = iterator.next();
                if (selectedNegPhrases1.contains(negPhrase)) {
                    iterator.remove();
                }
            }
            model.addAttribute("negPhrases", negPhrases);
            return "forecast";
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return "forecast";
        }
    }
    @PostMapping("/createNegList")
    public String createNegList (Model model) {
        model.addAttribute("negPhrases", selectedNegPhrases);
        model.addAttribute("searchQueries", selectedMetrics);
        if (addSystem.equals("Яндекс")) return "negList";
        else return "negListGoogle";
    }

    @PostMapping("/endCleaning")
    public String endCleaning(
            @RequestParam("negPhrases1") List<String> negPhrases1,
            @RequestParam("searchQueries1") List<String> searchQueries1,
            @RequestParam("negPhrases2") List<String> negPhrases2,
            @RequestParam("searchQueries2") List<String> searchQueries2,
            @RequestParam("negPhrasesTexts1") List<String> negPhrasesTexts1,
            @RequestParam("searchQueriesTexts1") List<String> searchQueriesTexts1,
            @RequestParam("negPhrasesTexts2") List<String> negPhrasesTexts2,
            @RequestParam("searchQueriesTexts2") List<String> searchQueriesTexts2
    ) {
                final List<String> negIdsCopy1 = removeSymbols(negPhrases1);
        final List<String> negIdsCopy2 = removeSymbols(negPhrases2);
        final List<String> queryIdsCopy1 = removeSymbols(searchQueries1);
        final List<String> queryIdsCopy2 = removeSymbols(searchQueries2);
        final List<String> negIdsCopyTexts1 = removeSymbols1(negPhrasesTexts1);
        final List<String> negIdsCopyTexts2 = removeSymbols1(negPhrasesTexts2);
        final List<String> queryIdsCopyTexts1 = removeSymbols1(searchQueriesTexts1);
        final List<String> queryIdsCopyTexts2 = removeSymbols1(searchQueriesTexts2);
        Calendar calendar = Calendar.getInstance();
        Date currentDate = calendar.getTime();
        Clean clean1 = new Clean();
        clean1.setUser(clean.getUser());
        clean1.setAdvertisingSystem(clean.getAdvertisingSystem());
        clean1.setProject(clean.getProject());
        clean1.setDate(currentDate);
        cleanRepository.save(clean1);
        List<NegPhrase> addedNegPhrases = selectedNegPhrases.stream()
                .filter(negPhrase -> negIdsCopy1.contains(String.valueOf(negPhrase.getId())))
                .collect(Collectors.toList());
        int index1 = 0;
        for (NegPhrase negPhrase:addedNegPhrases) {
            SearchQuery searchQuery = copySearchQuery(negPhrase.getSearchQuery());
            searchQuery.setClean(clean1);
            searchQueryRepository.save(searchQuery);
            NegPhrase newNegPhrase = new NegPhrase();
            newNegPhrase.setStatus("added");
            newNegPhrase.setText(negIdsCopyTexts1.get(index1));
            newNegPhrase.setSearchQuery(searchQuery);
            negPhraseRepository.save(newNegPhrase);
            for (Metric metric:selectedMetrics) {
                if (metric.getSearchQuery()==negPhrase.getSearchQuery()) {
                    Metric metric1 = copyMetric(metric);
                    metric1.setSearchQuery(searchQuery);
                    metricRepository.save(metric1);
                }
            }
            index1++;
        }
        List<NegPhrase> addedNegPhraseToCoordination = selectedNegPhrases.stream()
                .filter(negPhrase -> negIdsCopy2.contains(String.valueOf(negPhrase.getId())))
                .collect(Collectors.toList());
        int index2 = 0;
        for (NegPhrase negPhrase:addedNegPhraseToCoordination) {
            SearchQuery searchQuery = copySearchQuery(negPhrase.getSearchQuery());
            searchQuery.setClean(clean1);
            searchQueryRepository.save(searchQuery);
            NegPhrase newNegPhrase = new NegPhrase();
            newNegPhrase.setStatus("question");
            newNegPhrase.setText(negIdsCopyTexts2.get(index2));
            newNegPhrase.setSearchQuery(searchQuery);
            negPhraseRepository.save(newNegPhrase);
            for (Metric metric:selectedMetrics) {
                if (metric.getSearchQuery()==negPhrase.getSearchQuery()) {
                    Metric metric1 = copyMetric(metric);
                    metric1.setSearchQuery(searchQuery);
                    metricRepository.save(metric1);
                }
            }
            index2++;
        }
        List<Metric> addedQueries = selectedMetrics.stream()
                .filter(negPhrase -> queryIdsCopy1.contains(String.valueOf(negPhrase.getSearchQuery().getId())))
                .collect(Collectors.toList());
        int index3 = 0;
        for (Metric metric: addedQueries) {
            SearchQuery searchQuery = copySearchQuery(metric.getSearchQuery());
            searchQuery.setClean(clean1);
            searchQueryRepository.save(searchQuery);
            NegPhrase negPhrase = new NegPhrase();
            negPhrase.setStatus("added");
            negPhrase.setText(queryIdsCopyTexts1.get(index3));
            negPhrase.setSearchQuery(searchQuery);
            negPhraseRepository.save(negPhrase);
            Metric metric1 = copyMetric(metric);
            metric1.setSearchQuery(searchQuery);
            metricRepository.save(metric1);
            index3++;
        }
        List<Metric> addedQueriesToCoordination = selectedMetrics.stream()
                .filter(negPhrase -> queryIdsCopy2.contains(String.valueOf(negPhrase.getSearchQuery().getId())))
                .collect(Collectors.toList());
        int index4 = 0;
        for (Metric metric: addedQueriesToCoordination) {
            SearchQuery searchQuery = copySearchQuery(metric.getSearchQuery());
            searchQuery.setClean(clean1);
            searchQueryRepository.save(searchQuery);
            NegPhrase negPhrase = new NegPhrase();
            negPhrase.setStatus("question");
            negPhrase.setText(queryIdsCopyTexts2.get(index4));
            negPhrase.setSearchQuery(searchQuery);
            negPhraseRepository.save(negPhrase);
            Metric metric1 = copyMetric(metric);
            metric1.setSearchQuery(searchQuery);
            metricRepository.save(metric1);
            index4++;
        }
        return "home";
    }

    private SearchQuery copySearchQuery (SearchQuery old) {
        SearchQuery searchQuery = new SearchQuery();
        searchQuery.setCampaign(old.getCampaign());
        searchQuery.setAddGroup(old.getAddGroup());
        searchQuery.setKeyword(old.getKeyword());
        searchQuery.setText(old.getText());
        return searchQuery;
    }

    private Metric copyMetric (Metric old) {
        Metric metric = new Metric();
        metric.setShows(old.getShows());
        metric.setClicks(old.getClicks());
        metric.setConversions(old.getConversions());
        metric.setConsumption(old.getConsumption());
        metric.setCurrencyCode(old.getCurrencyCode());
        return metric;
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

    private List<String> removeSymbols(List<String> strings) {
        List<String> cleanedStrings = new ArrayList<>();
        for (String str : strings) {
            str = str.replaceAll("[\"\\[\\]]", "");
            cleanedStrings.add(str);
        }
        return cleanedStrings;
    }
    private List<String> removeSymbols1(List<String> strings) {
        List<String> cleanedStrings = new ArrayList<>();
        if(!strings.isEmpty())
        {
           if (strings.size()==1) {
               if (strings.get(0).length()!=2) {
                   String str = strings.get(0).substring(2, strings.get(0).length() - 2).replace("\\", "");
                   cleanedStrings.add(str);
               }
           } else {
               String str = strings.get(0).substring(2,strings.get(0).length()-1).replace("\\","");
               cleanedStrings.add(str);
               for (int i = 1; i<(strings.size()-1);i++) {
                   String str1 = strings.get(i).substring(1,strings.get(i).length()-1).replace("\\","");
                   cleanedStrings.add(str1);
               }
               String str2 = strings.get(strings.size()-1).substring(1,strings.get(strings.size()-1).length()-2).replace("\\","");
               cleanedStrings.add(str2);
           }
        }
        return cleanedStrings;
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
        articleList.add("и");
        articleList.add("или");
        articleList.add("с");
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

