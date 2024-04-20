package ru.angara.kitchen.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.sql.Timestamp;

@Entity
@Table(name="userDataTable")
@NoArgsConstructor
@Getter
@Setter
@ToString
public class User {
    @Id
    private Long chatId;
    private String firstName;
    private String lastName;
    private String userName;
    private Timestamp registeredAt;
    private Timestamp notificationAt;
    private Long inter; // в сек
}
