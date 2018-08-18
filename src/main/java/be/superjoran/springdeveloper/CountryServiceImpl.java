package be.superjoran.springdeveloper;

import org.springframework.stereotype.Service;

@Service
public class CountryServiceImpl implements CountryService {
    private final  CountryClient countryClient;

    public CountryServiceImpl(CountryClient countryClient) {
        this.countryClient = countryClient;
    }

    @Override
    public int getPopulation(String country) {
        return this.countryClient.getCountry(country).getCountry().getPopulation();
    }
}
