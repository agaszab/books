package pl.vxm.netino.books.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.vxm.netino.books.mod.Borrowed;

import java.sql.Date;
import java.util.List;

public interface BorrowedRepository  extends JpaRepository <Borrowed, Long>{

    List<Borrowed> findAllByIdpAndReturnedTrue(long idp);
    List<Borrowed> findAllByIdpAndReturnedFalse(long idp);
    Borrowed findByIdbAndReturnedFalse(long idb);
    List<Borrowed> findAllByReturnedFalse();
    List <Borrowed> findByDateBorrowingBeforeAndReturnedFalse(Date dateBorrowing);
    List <Borrowed> findByDateBorrowingAfterAndReturnedFalse(Date dateBorrowing);
}

