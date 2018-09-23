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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

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
        TreeSet<String> categories= new TreeSet<>();
        categories=categories();
        model.addAttribute("topTwoBookList", topTwoBookList);
        model.addAttribute("categories", categories);
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
        model.addAttribute("books", books);
        return "books";
    }


    @GetMapping("/book")
    public String book(Model model, @RequestParam long idb) {

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
            } else return "/nodata";
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
            return "redirect:/persons";
        } else return "/nodata";
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

        return "/books";
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
    public String returnBookRofPerson (@RequestParam long idb){

        Borrowed  borrowed = borrowedRepository.findByIdbAndReturnedFalse(idb);
        if (borrowed!=null) {
            borrowed.setReturned(true);
            borrowedRepository.save(borrowed);
        }

        return "redirect:/";
    }


    public List<Book> showBooksForPerson(long idp, boolean hist){
        List<Book> books= new ArrayList<>();
        List<Borrowed> borroweds;
        if (hist) borroweds = borrowedRepository.findAllByIdpAndReturnedFalse(idp);
          else borroweds=borrowedRepository.findAllByIdpAndReturnedTrue(idp);
        for (Borrowed elem:borroweds){
            Optional<Book> bookOptional = bookRepository.findById(elem.getIdb());
            if (bookOptional.isPresent()) {
                Book book = bookOptional.get();
                books.add(book);
            }}
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

}