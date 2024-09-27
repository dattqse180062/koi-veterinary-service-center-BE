package org.ftf.koifishveterinaryservicecenter.service.surchargeservice;

import org.ftf.koifishveterinaryservicecenter.dto.MovingSurchargeDTO;
import org.ftf.koifishveterinaryservicecenter.entity.MovingSurcharge;
import org.ftf.koifishveterinaryservicecenter.exception.MovingSurchargeNotFoundException;
import org.ftf.koifishveterinaryservicecenter.mapper.MovingSurchargeMapper;
import org.ftf.koifishveterinaryservicecenter.repository.MovingSurchargeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SurchargeServiceImpl implements SurchargeService {

    private final MovingSurchargeRepository movingSurchargeRepository;
//    private final MovingSurchargeMapper movingSurchargeMapper;

    @Autowired
    public SurchargeServiceImpl(MovingSurchargeRepository movingSurchargeRepository/*, MovingSurchargeMapper movingSurchargeMapper*/) {
        this.movingSurchargeRepository = movingSurchargeRepository;
//        this.movingSurchargeMapper = movingSurchargeMapper;
    }

    /*
    * Get all MovingSurcharge from database
    * */
    @Override
    public List<MovingSurcharge> getAllMovingSurcharges() {

        List<MovingSurcharge> movingSurcharges = movingSurchargeRepository.findAll();

        // Map List of MovingSurCharge Entity into MovingSurcharge DTO
//        List<MovingSurchargeDTO> movingSurchargeDTOs = movingSurcharges.stream()
//                .map(movingSurchargeMapper::convertToMovingSurchargeDTO)
//                .collect(Collectors.toList());

        return movingSurcharges;
    }

    /*
    * Get MovingSurcharge By ID
    * */
    @Override
    public MovingSurcharge getMovingSurchargeById(Integer id) throws MovingSurchargeNotFoundException {

        MovingSurcharge movingSurcharge = movingSurchargeRepository.findById(id).orElse(null);

        if(movingSurcharge == null) { // MovingSurcharge not existed with given id
            throw new MovingSurchargeNotFoundException("Moving Surcharge not found with id: " + id);
        } else { // MovingSurcharge existed
            return movingSurcharge;
        }

    }

    @Override
    public MovingSurcharge updateMovingSurcharge(Integer movingSurchargeId, MovingSurcharge movingSurchargeFromRequest) {
        MovingSurcharge movingSurcharge = movingSurchargeRepository.findById(movingSurchargeId).orElse(null);

        if (movingSurcharge == null) { // Moving surcharge not existed
            throw new MovingSurchargeNotFoundException("Moving Surcharge not found with id: " + movingSurchargeId);
        } else { // Moving surcharge existed
//            movingSurcharge = movingSurchargeMapper.convertToMovingSurcharge(movingSurchargeFromRequest); // Convert MovingSurcharge DTO into Entity
            movingSurchargeRepository.save(movingSurchargeFromRequest); // Update into database
            return movingSurchargeFromRequest;
        }
    }
}