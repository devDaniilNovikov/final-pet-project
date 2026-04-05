package dn.accountservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.UUID;


@Getter
@Setter
@Table(schema = "marketplace",name = "address")
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class AddressEntity {


    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id",nullable = false)
    private UUID id;

    @Column(name = "street",nullable = false)
    private String street;

    @Column(name = "building",nullable = false)
    private String building;

    @Column(name = "city",nullable = false)
    private String city;

    @Column(name = "zip_code",nullable = false)
    private String zipCode;

    @Column(name = "apartment")
    private String apartment;

    @Column(name = "comment")
    private String commentForCourier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private AccountEntity account;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AddressEntity that = (AddressEntity) o;
        return new EqualsBuilder()
                .append(id, that.id)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(id)
                .toHashCode();
    }
}
