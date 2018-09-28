package pl.vxm.netino.books.controllers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import pl.vxm.netino.books.mod.Book;
import pl.vxm.netino.books.mod.Borrowed;
import pl.vxm.netino.books.mod.Person;
import pl.vxm.netino.books.repository.BookRepository;
import pl.vxm.netino.books.repository.BorrowedRepository;
import pl.vxm.netino.books.repository.PersonRepository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

@Controller
public class MainController {


    private BookRepository bookRepository;
    private PersonRepository personRepository;
    private BorrowedRepository borrowedRepository;

    public MainController(BookRepository bookRepository, PersonRepository personRepository, BorrowedRepository borrowedRepository) {
        this.bookRepository = bookRepository;
        this.personRepository = personRepository;
        this.borrowedRepository = borrowedRepository;
    }

    @GetMapping("/")
    public String main(Model model) {
        Pageable pageable = new PageRequest(0, 2, Sort.Direction.DESC, "idb");
        Page<Book> books=bookRepository.findAll(pageable);
        List<Book> topTwoBookList = books.getContent();
        TreeSet<String> categories;
        categories=categories();
        model.addAttribute("categories", categories);
        model.addAttribute("topTwoBookList", topTwoBookList);

        return "index";
    }

    @GetMapping("/people")
    public String persons(Model model) {
        List<Person> persons = personRepository.findAll();
        model.addAttribute("persons", persons);
        model.addAttribute("hist", true);
        return "people";
    }


    @GetMapping("/person")
    public String persons(Model model, boolean hist, @RequestParam long idp) {

        Optional<Person> personOptional = personRepository.findById(idp);

        if (personOptional.isPresent()) {
            Person person = personOptional.get();
            model.addAttribute("person", person);
        } else {
            return "redirect:/";
        }

        List<Book> books;
        books=showBooksForPerson(idp, hist);
        model.addAttribute("hist", hist);
        model.addAttribute("books", books);
        if (books.size()>0 && hist==true) model.addAttribute("bookHead", "Aktualnie wypożyczone książki:");
        else if (books.size()==0 && hist==true) model.addAttribute("bookHead", "Obecnie brak wypożyczeń");
        else if (books.size()>0 && hist==false) model.addAttribute("bookHead", "Historia wypożyczeń:");
        else model.addAttribute("bookHead", "Ta osoba nigdy nic wcześniej nie wypożyczała.");
        return "person";
    }




    @GetMapping("/books")
    public String books(Model model) {
        List<Book> books = bookRepository.findAll();
        TreeSet<String> categories;
        categories=categories();
        model.addAttribute("categories", categories);
        model.addAttribute("books", books);
        return "books";
    }


    @GetMapping("/book")
    public String book(Model model, @RequestParam long idb) {

        TreeSet<String> categories;
        categories=categories();
        model.addAttribute("categories", categories);

        Optional<Book> bookOptional = bookRepository.findById(idb);
        Borrowed borrowed = borrowedRepository.findByIdbAndReturnedFalse(idb);
        if (bookOptional.isPresent()) {
            if (borrowed!=null) {
                model.addAttribute("pom", true);
                Optional<Person> personOptional = personRepository.findById(borrowed.getIdp());
                if (bookOptional.isPresent()) {
                    Person person = personOptional.get();
                    model.addAttribute("person", person);
                    model.addAttribute("borrowed", borrowed);
                }
            }
             else {
            List<Person> persons = personRepository.findAll();
            model.addAttribute("persons", persons);
            model.addAttribute("pom", false);
        }

                Book book = bookOptional.get();
                model.addAttribute("newBorrowed", new Borrowed());
                model.addAttribute("book", book);

            } else {
                return "redirect:/";
            }

        return "/book";
    }


    @GetMapping("/borroweds")
    public String borroweds (Model model, char time) {

        LocalDate today = LocalDate.now();
        Date sqlDate = Date.valueOf(today.minusDays(30));
        List<Borrowed> borroweds;
        model.addAttribute("sqlDate", sqlDate);
        switch (time){
            case '1': borroweds = borrowedRepository.findByDateBorrowingAfterAndReturnedFalse(sqlDate); break;
            case '0': borroweds = borrowedRepository.findByDateBorrowingBeforeAndReturnedFalse(sqlDate);  break;
            default: borroweds = borrowedRepository.findAllByReturnedFalse(); break;
        }
        TreeSet<String> categories;
        categories=categories();
        List books =new ArrayList();
        addListBook (borroweds, books);
        model.addAttribute("categories", categories);
        model.addAttribute("books", books);
        return "books";
    }


    @GetMapping("/addbook")
    public String addbook(Model model) {
        model.addAttribute("newBook", new Book());
        return "/addbook";
    }

    @PostMapping("/addb")
    public String addb(Book book) {
        if (!book.getAuthor().equals("") && !book.getTitle().equals("")) {
            bookRepository.save(book);
            return "redirect:/books";
        } else return "/nodata";
    }

    @GetMapping("/editbook")
    public String editBookForm(@RequestParam Long idb, Model model) {

        Optional<Book> bookOptional = bookRepository.findById(idb);

        if (bookOptional.isPresent()) {
            Book book = bookOptional.get();
            model.addAttribute("book", book);
            return "editbook";
        }
        return "/";
    }

    @PostMapping("/editb")
    public String editBook(Book book, @RequestParam Long idb) {
        Optional<Book> bookOptional = bookRepository.findById(idb);
        if (bookOptional.isPresent()) {
            Book newBook = bookOptional.get();
            if (!book.getAuthor().equals("") && !book.getTitle().equals("")) {
                newBook.setAuthor(book.getAuthor());
                newBook.setTitle(book.getTitle());
                newBook.setPublisher(book.getPublisher());
                newBook.setPublicationYear(book.getPublicationYear());
                newBook.setCategory(book.getCategory());
                bookRepository.save(newBook);
                return "redirect:/books";
            } else return "/nodata";
        }
        return "redirect:/books";
    }

    @GetMapping("/editperson")
    public String editPeopleForm(@RequestParam Long idp, Model model) {


        Optional<Person> personOptional = personRepository.findById(idp);

        if (personOptional.isPresent()) {
            Person person = personOptional.get();
            model.addAttribute("person", person);
            return "editperson";
        }
        return "/";
    }

    @PostMapping("/editp")
    public String editP(Person person, @RequestParam Long idp) {
        Optional<Person> personOptional = personRepository.findById(idp);
        if (personOptional.isPresent()) {
            Person newPerson = personOptional.get();
            if (!person.getFirstName().equals("") && !person.getLastName().equals("")) {
                newPerson.setFirstName(person.getFirstName());
                newPerson.setLastName(person.getLastName());
                newPerson.setPhone(person.getPhone());
                personRepository.save(newPerson);
                return "/people";
            } else return "nodata";
        }
        return "/people";
    }


    @GetMapping("/addperson")
    public String addperson(Model model) {
        model.addAttribute("newPerson", new Person());
        return "/addperson";
    }

    @PostMapping("/addp")
    public String addp(Person person) {
        if (!person.getFirstName().equals("") && !person.getLastName().equals("")) {
            personRepository.save(person);
            return "/people";
        } else return "nodata";
    }

    @PostMapping("/addborrowed")
    public String newBorrowed(Borrowed borrowed, Person person) {

            borrowed.setIdb(borrowed.getIdb());
            borrowed.setIdp(person.getIdp());
            borrowed.setReturned(false);
            LocalDate today = LocalDate.now();
            Date sqlDate = Date.valueOf( today );
            borrowed.setDateBorrowing(sqlDate);
            borrowedRepository.save(borrowed);

        return "redirect: /books";
    }


    @GetMapping("/returnbook")
    public String returnBook (@RequestParam long id){

        Optional <Borrowed>  borrowedOptional = borrowedRepository.findById(id);
        if (borrowedOptional.isPresent()) {
        Borrowed borrowed=borrowedOptional.get();
        borrowed.setReturned(true);
        borrowedRepository.save(borrowed);
        }

        return "redirect:/";
    }

    @GetMapping("/returnbooks")
    public String returnBookFofPerson (Model model, @RequestParam long idb){

        Borrowed  borrowed = borrowedRepository.findByIdbAndReturnedFalse(idb);
        if (borrowed!=null) {
            borrowed.setReturned(true);
            borrowedRepository.save(borrowed);
            return "redirect:/people";
        }

        return "/noresult";
    }


    @GetMapping("/bookcategory")
        public String bookCategory (Model model, @RequestParam String cathegory){
        List <Book> books;
        books=bookRepository.findAllByCategory(cathegory);
        TreeSet<String> categories;
        categories=categories();
        model.addAttribute("cathegory", cathegory);
        model.addAttribute("categories", categories);
        model.addAttribute("books", books);
        return "books";
        }

    @GetMapping("/searchpersonfirst")
       public String searchPersonF(Model model, @RequestParam String fname){
        List <Person> persons=searchPersons("fname", fname);
        if (persons.isEmpty()) return "noresult";
        model.addAttribute("persons", persons);
        model.addAttribute("hist", true);
        return "people";
            }

    @GetMapping("/searchpersonlast")
    public String searchPersonL(Model model, @RequestParam String lname){
        List <Person> persons=searchPersons("lname", lname);
        if (persons.isEmpty()) return "noresult";
        model.addAttribute("persons", persons);
        model.addAttribute("hist", true);
        return "people";
    }

    @GetMapping("/searchtitle")
    public String searchTitle(Model model, @RequestParam String title){
        List <Book> books=searchBooks("title", title);
        if (books.isEmpty()){
            model.addAttribute("empty", true);
            return "noresult";
        }
        TreeSet<String> categories;
        categories=categories();
        model.addAttribute("categories", categories);
        model.addAttribute("books", books);
        return "books";
    }

    @GetMapping("/searchauthor")
    public String searchAuthorn(Model model, @RequestParam String author){
        List <Book> books=searchBooks("author", author);
        if (books.isEmpty()) {
            model.addAttribute("empty", true);
            return "noresult";}
        TreeSet<String> categories;
        categories=categories();
        model.addAttribute("categories", categories);
        model.addAttribute("books", books);
        return "books";
    }



        public List<Book> showBooksForPerson(long idp, boolean hist){
        List<Book> books= new ArrayList<>();
        List<Borrowed> borroweds;
        if (hist) borroweds = borrowedRepository.findAllByIdpAndReturnedFalse(idp);
          else borroweds=borrowedRepository.findAllByIdpAndReturnedTrue(idp);
          addListBook (borroweds, books);
     return books;
    }

    public TreeSet <String> categories(){

        List <Book> books=bookRepository.findAll();
        TreeSet<String> categories= new TreeSet<>();
        for (Book elem:books){
            categories.add(elem.getCategory());
        }
        return categories;
    }

    public List <Book> searchBooks(String name, String searchBook){

        List <Book> allBooks=bookRepository.findAll();
        List <Book> books = new ArrayList<>();

        for (Book elem:allBooks){
            searchBook.toLowerCase();
            String pom;
            if (name.equals("title")) pom=elem.getTitle().toLowerCase();
            else pom=elem.getAuthor().toLowerCase();
            if (pom.contains(searchBook)) books.add(elem);
        }

        return books;
    }

    public List <Person> searchPersons(String name, String searchPerson){

        List <Person> persons = new ArrayList<>();
        List <Person> allPersons=personRepository.findAll();

        for (Person elem:allPersons){
        searchPerson.toLowerCase();
        String pom;
        if (name.equals("lname")) pom=elem.getLastName().toLowerCase();
        else pom=elem.getFirstName().toLowerCase();
        if (pom.contains(searchPerson)) persons.add(elem);
    }
        return persons;
    }

    /*
     public String searchPersonL(Model model, @RequestParam String lname){

        List <Person> persons = new ArrayList<>();
        List <Person> allPersons=personRepository.findAll();

        for (Person elem:allPersons){
            String pom=elem.getLastName().toLowerCase();
            String pomLname=lname.toLowerCase();
            if (pom.contains(pomLname)) persons.add(elem);
        }

     public String searchPersonF(Model model, @RequestParam String fname){

        List <Person> persons = new ArrayList<>();
        List <Person> allPersons=personRepository.findAll();

        for (Person elem:allPersons){
            String pom=elem.getFirstName().toLowerCase();
            String pomFname=fname.toLowerCase();
            if (pom.contains(pomFname)) persons.add(elem);
        }

        */

    public void addListBook (List<Borrowed> borroweds, List <Book> books){
    for (Borrowed elem:borroweds) {
        Optional<Book> bookOptional = bookRepository.findById(elem.getIdb());
        if (bookOptional.isPresent()) {
            Book book = bookOptional.get();
            books.add(book);
        }
    }
    }
}
