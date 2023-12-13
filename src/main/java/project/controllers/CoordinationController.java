package project.controllers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import project.models.Clean;
import project.models.Metric;
import project.models.NegPhrase;
import project.models.SearchQuery;
import project.repositories.*;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class CoordinationController {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SearchQueryRepository searchQueryRepository;
    @Autowired
    private CleanRepository cleanRepository;
    @Autowired
    private NegPhraseRepository negPhraseRepository;
    List<NegPhrase> negPhrases = new ArrayList<>();

    List<NegPhrase> negPhrasesAnswer = new ArrayList<>();
    @GetMapping("/messagesBoss")
    public String messagesBoss(Model model) {
        List<Clean> cleans = cleanRepository.findAll();
        List<Clean> cleans1 = new ArrayList<>();
        for (Clean clean: cleans) {
            List<NegPhrase> negPhrases1 = new ArrayList<>();
            List<SearchQuery> searchQueries = searchQueryRepository.findAllByClean(clean);
            for (SearchQuery searchQuery: searchQueries) {
                List<NegPhrase> negPhrases = negPhraseRepository.findAllBySearchQuery(searchQuery);
                for (NegPhrase negphrase:negPhrases) {
                    if(negphrase.getStatus().equals("question"))
                        negPhrases1.add(negphrase);
                }
            }
            if (!negPhrases1.isEmpty()) cleans1.add(clean);
        }
        model.addAttribute("cleans", cleans1);
        return "messagesBoss";
    }
    @GetMapping("/messagesSpec")
    public String messagesSpec(Model model) {
        List<Clean> cleans = cleanRepository.findAll();
        List<Clean> cleans1 = new ArrayList<>();
        for (Clean clean: cleans) {
            List<NegPhrase> negPhrases1 = new ArrayList<>();
            List<SearchQuery> searchQueries = searchQueryRepository.findAllByClean(clean);
            for (SearchQuery searchQuery: searchQueries) {
                List<NegPhrase> negPhrases = negPhraseRepository.findAllBySearchQuery(searchQuery);
                for (NegPhrase negphrase:negPhrases) {
                    if(negphrase.getStatus().equals("agreed"))
                        negPhrases1.add(negphrase);
                }
            }
            if (!negPhrases1.isEmpty()) cleans1.add(clean);
        }
        model.addAttribute("cleans", cleans1);
        return "messagesSpec";
    }
    @PostMapping("/questionCard")
    public String questionCard(Model model, @RequestParam("cleanId") String id) {
        Clean clean = cleanRepository.getReferenceById(Long.valueOf(id));
            List<NegPhrase> negPhrases1 = new ArrayList<>();
            List<SearchQuery> searchQueries = searchQueryRepository.findAllByClean(clean);
            for (SearchQuery searchQuery: searchQueries) {
                List<NegPhrase> negPhrases2 = negPhraseRepository.findAllBySearchQuery(searchQuery);
                for (NegPhrase negPhrase: negPhrases2) {
                    if (negPhrase.getStatus().equals("question"))
                        negPhrases1.add(negPhrase);
                }
            }
            negPhrases = negPhrases1;
        model.addAttribute("negPhrases", negPhrases1);
        if (clean.getAdvertisingSystem().contains("Яндекс"))
        return "questionList";
        else return "questionListGoogle";
    }
    @PostMapping("/answerCard")
    public String answerCard(Model model, @RequestParam("cleanId") String id) {
        Clean clean = cleanRepository.getReferenceById(Long.valueOf(id));
        List<NegPhrase> negPhrases1 = new ArrayList<>();
        List<SearchQuery> searchQueries = searchQueryRepository.findAllByClean(clean);
        for (SearchQuery searchQuery: searchQueries) {
            List<NegPhrase> negPhrases2 = negPhraseRepository.findAllBySearchQuery(searchQuery);
            for (NegPhrase negPhrase: negPhrases2) {
                if (negPhrase.getStatus().equals("agreed"))
                    negPhrases1.add(negPhrase);
            }
        }
        negPhrasesAnswer = negPhrases1;
        model.addAttribute("negPhrases", negPhrasesAnswer);
        return "answersList";
    }
    @PostMapping("/answerSeen")
    public String answerSeen(Model model) {
        for (NegPhrase negPhrase:negPhrasesAnswer) {
            negPhrase.setStatus("added");
            negPhraseRepository.save(negPhrase);
        }
        List<Clean> cleans = cleanRepository.findAll();
        List<Clean> cleans1 = new ArrayList<>();
        for (Clean clean: cleans) {
            List<NegPhrase> negPhrases1 = new ArrayList<>();
            List<SearchQuery> searchQueries = searchQueryRepository.findAllByClean(clean);
            for (SearchQuery searchQuery: searchQueries) {
                List<NegPhrase> negPhrases = negPhraseRepository.findAllBySearchQuery(searchQuery);
                for (NegPhrase negphrase:negPhrases) {
                    if(negphrase.getStatus().equals("agreed"))
                        negPhrases1.add(negphrase);
                }
            }
            if (!negPhrases1.isEmpty()) cleans1.add(clean);
        }
        model.addAttribute("cleans", cleans1);
        return "messagesSpec";
    }

    @PostMapping("/answer")
    public String answer(Model model, @RequestParam("addedNegPhrases") List<String> addedNegPhrases, @RequestParam("negPhrasesTexts") List<String> negPhrasesTexts) {
        final List<String> negIdsCopy1 = removeSymbols(addedNegPhrases);
        final List<String> negIdsCopyTexts1 = removeSymbols1(negPhrasesTexts);
        List<NegPhrase> addedPhrases = negPhrases.stream()
                .filter(negPhrase -> negIdsCopy1.contains(String.valueOf(negPhrase.getId())))
                .collect(Collectors.toList());
        int index = 0;
        for (NegPhrase negPhrase: addedPhrases) {
            negPhrase.setText(negIdsCopyTexts1.get(0));
            negPhrase.setStatus("agreed");
            negPhraseRepository.save(negPhrase);
            index++;
        }
        List<NegPhrase> negPhrasesToDelete = new ArrayList<>();
        for (NegPhrase negPhrase: negPhrases) {
            int i =0;
            for (NegPhrase neg: addedPhrases) {
                if (neg.equals(negPhrase)) i++;
            }
            if (i==0) negPhrasesToDelete.add(negPhrase);
        }
        for (NegPhrase n: negPhrasesToDelete) {
            negPhraseRepository.delete(n);
        }
        List<Clean> cleans = cleanRepository.findAll();
        List<Clean> cleans1 = new ArrayList<>();
        for (Clean clean: cleans) {
            List<NegPhrase> negPhrases1 = new ArrayList<>();
            List<SearchQuery> searchQueries = searchQueryRepository.findAllByClean(clean);
            for (SearchQuery searchQuery: searchQueries) {
                List<NegPhrase> negPhrases = negPhraseRepository.findAllBySearchQuery(searchQuery);
                for (NegPhrase negphrase:negPhrases) {
                    if(negphrase.getStatus().equals("question"))
                        negPhrases1.add(negphrase);
                }
            }
            if (!negPhrases1.isEmpty()) cleans1.add(clean);
        }
        model.addAttribute("cleans", cleans1);
        return "messagesBoss";
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
}
