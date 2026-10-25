package archfixtures.application.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class OrmInDomain {

    @Id
    Long id;
}
