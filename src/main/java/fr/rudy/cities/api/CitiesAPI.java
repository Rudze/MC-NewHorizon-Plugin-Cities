package fr.rudy.cities.api;

import fr.rudy.cities.manager.CityManager;
import fr.rudy.cities.manager.ClaimManager;


public interface CitiesAPI {
    CityManager getCityManager();
    ClaimManager getClaimManager();

}

