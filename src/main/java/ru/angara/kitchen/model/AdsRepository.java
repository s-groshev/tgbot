package ru.angara.kitchen.model;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdsRepository extends CrudRepository<Ads, Long> {
}
