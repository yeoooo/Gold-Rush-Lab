package io.devyeoooo.Gold_Rush_Lab_Consumer.mine.repository.entity;

import io.devyeoooo.Gold_Rush_Lab_Consumer.mine.exception.MineDepletedException;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "mine")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MineEntity {

    @Id
    private Long id;

    private Long remainingAmount;

    public void mine(Long amount) {
        if (remainingAmount < amount) {
            throw new MineDepletedException(id);
        }
        remainingAmount -= amount;
    }
}
