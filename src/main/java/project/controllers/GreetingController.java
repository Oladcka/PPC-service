package project.controllers;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import project.models.Clean;
import project.models.Metric;
import project.models.SearchQuery;
import project.models.Users;
import project.repositories.PersonRepository;
import project.repositories.UserRepository;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
public class GreetingController {

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

    private Clean clean;

    @PostMapping("/loadReport")
    public String loadReport (Model model, @RequestParam("fileInput") MultipartFile[] reports, @RequestParam("name") String name, @RequestParam("system") String system) throws IOException {
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
                        model.addAttribute("metrics", metrics);
                    }

                }
                return "cleaning";
            }
            else {
                model.addAttribute("error", "ошибка");
                return "loadReport";
            }
        } else {
            if (checkGoogleReport(workbookNew))  return "loadReport";
            else return "home";
        }

//        try (BufferedOutputStream fio = new BufferedOutputStream(new FileOutputStream(UPLOAD_DIRECTORY))) {
//            workbookNew.write(fio);
//        }
 //       out.close();
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


}
