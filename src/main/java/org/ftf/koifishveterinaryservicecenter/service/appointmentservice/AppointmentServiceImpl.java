package org.ftf.koifishveterinaryservicecenter.service.appointmentservice;

import org.ftf.koifishveterinaryservicecenter.dto.appointment.AppointmentUpdateDto;
import org.ftf.koifishveterinaryservicecenter.entity.*;
import org.ftf.koifishveterinaryservicecenter.entity.veterinarian_slots.VeterinarianSlots;
import org.ftf.koifishveterinaryservicecenter.enums.AppointmentStatus;
import org.ftf.koifishveterinaryservicecenter.enums.PaymentMethod;
import org.ftf.koifishveterinaryservicecenter.enums.PaymentStatus;
import org.ftf.koifishveterinaryservicecenter.enums.SlotStatus;
import org.ftf.koifishveterinaryservicecenter.exception.*;
import org.ftf.koifishveterinaryservicecenter.repository.AppointmentRepository;
import org.ftf.koifishveterinaryservicecenter.repository.MedicalReportRepository;
import org.ftf.koifishveterinaryservicecenter.repository.VeterinarianSlotsRepository;
import org.ftf.koifishveterinaryservicecenter.service.addressservice.AddressService;
import org.ftf.koifishveterinaryservicecenter.service.appointmentservice.appointmentstate.AppointmentContext;
import org.ftf.koifishveterinaryservicecenter.service.appointmentservice.appointmentstate.AppointmentStateFactory;
import org.ftf.koifishveterinaryservicecenter.service.emailservice.EmailService;
import org.ftf.koifishveterinaryservicecenter.service.feedbackservice.FeedbackService;
import org.ftf.koifishveterinaryservicecenter.service.fishservice.FishService;
import org.ftf.koifishveterinaryservicecenter.service.medicalreportservice.MedicalReportService;
import org.ftf.koifishveterinaryservicecenter.service.paymentservice.PaymentService;
import org.ftf.koifishveterinaryservicecenter.service.serviceservice.ServiceService;
import org.ftf.koifishveterinaryservicecenter.service.slotservice.SlotService;
import org.ftf.koifishveterinaryservicecenter.service.surchargeservice.SurchargeService;
import org.ftf.koifishveterinaryservicecenter.service.userservice.AuthenticationService;
import org.ftf.koifishveterinaryservicecenter.service.userservice.UserService;
import org.ftf.koifishveterinaryservicecenter.service.voucherservice.VoucherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final MedicalReportService medicalReportService;
    private final UserService userService;
    private final MedicalReportRepository medicalReportRepository;
    private final ServiceService serviceService;
    private final SlotService slotService;
    private final PaymentService paymentService;
    private final AuthenticationService authenticationService;
    private final VeterinarianSlotsRepository veterinarianSlotsRepository;
    private final AddressService addressService;
    private final SurchargeService surchargeService;
    private final FishService fishService;
    private final FeedbackService feedbackService;
    private final AppointmentStateFactory appointmentStateFactory;
    private final EmailService emailService;
    private final VoucherService voucherService;


    @Autowired
    public AppointmentServiceImpl(AppointmentRepository appointmentRepository, MedicalReportService medicalReportService, UserService userService, MedicalReportRepository medicalReportRepository, ServiceService serviceService, SlotService slotService, PaymentService paymentService, AuthenticationService authenticationService, VeterinarianSlotsRepository veterinarianSlotsRepository, AddressService addressService, SurchargeService surchargeService, FishService fishService, FeedbackService feedbackService, AppointmentStateFactory appointmentStateFactory, EmailService emailService, VoucherService voucherService) {
        this.appointmentRepository = appointmentRepository;
        this.medicalReportService = medicalReportService;
        this.userService = userService;
        this.medicalReportRepository = medicalReportRepository;
        this.serviceService = serviceService;
        this.slotService = slotService;
        this.paymentService = paymentService;
        this.authenticationService = authenticationService;
        this.veterinarianSlotsRepository = veterinarianSlotsRepository;
        this.addressService = addressService;
        this.surchargeService = surchargeService;
        this.fishService = fishService;
        this.feedbackService = feedbackService;
        this.appointmentStateFactory = appointmentStateFactory;
        this.emailService = emailService;
        this.voucherService = voucherService;
    }


    @Override
    public void createMedicalReport(MedicalReport medicalReport, Integer appointmentId) {

        Integer prescriptionId = medicalReport.getPrescription().getPrescriptionId();
        if (prescriptionId != null) {
            // get prescription on db by id
            Prescription prescription = medicalReportService.findPrescriptionById(prescriptionId);

            // set prop for medicalReport
            medicalReport.setPrescription(prescription);

        } else medicalReport.setPrescription(null);

        Integer veterinarianId = authenticationService.getAuthenticatedUserId();
        User veterinarian = userService.getVeterinarianById(veterinarianId);

        medicalReport.setVeterinarian(veterinarian);
        MedicalReport savedMedicalReport = medicalReportRepository.save(medicalReport);

        Appointment appointment = getAppointmentById(appointmentId);
        appointment.setMedicalReport(savedMedicalReport);

        appointmentRepository.save(appointment);
    }

    @Override
    public List<Status> findStatusByAppointmentId(Integer appointmentId) throws AppointmentNotFoundException {

        // Get appointment by Appointment Id - Not found => exception
        Appointment appointment = this.getAppointmentById(appointmentId);

        // Get status list
        List<Status> statuses = new ArrayList<>(appointment.getStatuses());
        if (statuses.isEmpty()) {
            throw new StatusNotFoundException("Not found status logs of Appointment with id: " + appointmentId);
        }

        // Sort status by Id
        statuses.sort(Comparator.comparing(Status::getStatusId));

        return statuses;
    }

    public void createAppointment(Appointment appointment, Integer customerId) {
        // 1. online booking
        // 2. consultation at home

        // setting fields for newAppointment
        Appointment newAppointment = new Appointment();

        // create_date
        newAppointment.setCreatedDate(LocalDateTime.now());

        // service_id
        org.ftf.koifishveterinaryservicecenter.entity.Service bookedService = serviceService.getServiceById(appointment.getService().getServiceId());
        newAppointment.setService(bookedService);

        // address_id
        Integer addressId = appointment.getAddress().getAddressId();
        if (addressId != null) {
            Address address = addressService.getAddressById(addressId);
            newAppointment.setAddress(address);

            // moving_surcharge_id
            MovingSurcharge movingSurcharge = surchargeService.getMovingSurchargeFromAddressId(addressId);
            newAppointment.setMovingSurcharge(movingSurcharge);
        }

        // slot_id
        TimeSlot timeSlot = slotService.getTimeSlotById(appointment.getTimeSlot().getSlotId());
        slotService.updateVeterinarianSlotsStatus(appointment.getVeterinarian().getUserId(), timeSlot.getSlotId(), SlotStatus.BOOKED);
        newAppointment.setTimeSlot(timeSlot);

        // user_id
        User userFromDb = userService.getCustomerById(customerId);
        newAppointment.setCustomer(userFromDb);

        // veterinarian_id
        if (appointment.getVeterinarian().getUserId() != null) {
            User veterinarianFromDb = userService.getVeterinarianById(appointment.getVeterinarian().getUserId());
            newAppointment.setVeterinarian(veterinarianFromDb);

            // as staff confirmed, vet_slot turns to 'BOOKED'
        }

        // email
        newAppointment.setEmail(appointment.getEmail());

        // phone
        newAppointment.setPhoneNumber(appointment.getPhoneNumber());

        // current_status : **
        newAppointment.setCurrentStatus(AppointmentStatus.PENDING);

        // customer_name
        newAppointment.setCustomerName(appointment.getCustomerName());

        // description
        newAppointment.setDescription(appointment.getDescription());

        // fish
        Integer fishId = appointment.getFish().getFishId();
        if (fishId != null) {
            Fish fish = fishService.getFishById(fishId);
            newAppointment.setFish(fish);
        }

        // voucher
        Integer voucherId = appointment.getVoucher().getId();
        if (voucherId != null) {
            Integer subtractedVoucherId = voucherService.subtractAVoucherOfCustomer(customerId, appointment.getVoucher().getId());
            Voucher voucher = voucherService.findVoucherById(subtractedVoucherId);
            newAppointment.setVoucher(voucher);
        }

        // total price
        newAppointment.setTotalPrice(calculatePrice(newAppointment));

        // payment_id
        Payment payment = appointment.getPayment();
        payment.setAmount(newAppointment.getTotalPrice());
        Payment savedPayment = paymentService.createPayment(payment);
        newAppointment.setPayment(savedPayment);

        appointmentRepository.save(newAppointment);
    }

    @Override
    public List<Appointment> getAppointmentsByCustomerId(Integer customerId) {
        List<Appointment> appointments = appointmentRepository.findAppointmentByCustomerId(customerId);
        if (appointments.isEmpty()) {
            throw new AppointmentNotFoundException("Appointment not found!");
        }
        // Sort by newest appointment
        appointments.sort(Comparator.comparing(Appointment::getAppointmentId).reversed());

        return appointments;
    }

    @Override
    public List<Appointment> getAllAppointments() {
        List<Appointment> appointments = appointmentRepository.findAll();
        if (appointments.isEmpty()) {
            throw new AppointmentNotFoundException("Not found appointments");
        }
        // Sort by created date
        appointments.sort(Comparator.comparing(Appointment::getAppointmentId).reversed());

        return appointments;
    }


    @Override
    public Appointment updateAppointment(AppointmentUpdateDto appointmentDto, Integer appointmentId) throws AppointmentUpdatedException {

        Integer authenticatedUserId = authenticationService.getAuthenticatedUserId();

        Appointment bookedAppointment = getAppointmentById(appointmentId);

        if (isAbleToUpdateAppointment(bookedAppointment, appointmentDto)) {
            // update slot Id
            if (appointmentDto.getSlotId() != null) {
                // update current vet_slot to AVAILABLE
                slotService.updateVeterinarianSlotsStatus(bookedAppointment.getVeterinarian().getUserId(), bookedAppointment.getTimeSlot().getSlotId(), SlotStatus.AVAILABLE);

                TimeSlot newTimeSlot = slotService.getTimeSlotById(appointmentDto.getSlotId());
                bookedAppointment.setTimeSlot(newTimeSlot);


                // update new vet_slot to BOOKED
                slotService.updateVeterinarianSlotsStatus(bookedAppointment.getVeterinarian().getUserId(), bookedAppointment.getTimeSlot().getSlotId(), SlotStatus.BOOKED);
            }

            if (appointmentDto.getVeterinarianId() != null) {
                // update current vet_slot to AVAILABLE
                slotService.updateVeterinarianSlotsStatus(bookedAppointment.getVeterinarian().getUserId(), bookedAppointment.getTimeSlot().getSlotId(), SlotStatus.AVAILABLE);

                User veterinarian = userService.getVeterinarianById(appointmentDto.getVeterinarianId());
                bookedAppointment.setVeterinarian(veterinarian);

                // update new vet_slot to BOOKED
                slotService.updateVeterinarianSlotsStatus(bookedAppointment.getVeterinarian().getUserId(), bookedAppointment.getTimeSlot().getSlotId(), SlotStatus.BOOKED);
            }


            // update email
            if (appointmentDto.getEmail() != null) {
                bookedAppointment.setEmail(appointmentDto.getEmail());
            }

            // update phone number
            if (appointmentDto.getPhoneNumber() != null) {
                bookedAppointment.setPhoneNumber(appointmentDto.getPhoneNumber());
            }

            return appointmentRepository.save(bookedAppointment);
        }
        throw new AppointmentUpdatedException("Cannot update this appointment. The appointment is upcoming soon");
    }


    @Override
    public void cancelAppointment(Integer appointmentId) {

        Integer cancelledActorId = authenticationService.getAuthenticatedUserId();
        User cancelledActor = userService.getUserProfile(cancelledActorId);

        Optional<Appointment> appointment = appointmentRepository.findById(appointmentId);
        if (appointment.isEmpty())
            throw new AppointmentNotFoundException("Appointment not found with id: " + appointmentId);

        if (appointment.get().getVeterinarian() != null && cancelledActor.getRole().getRoleKey().equals("CUS")) {
            Integer veterinarianId = appointment.get().getVeterinarian().getUserId();
            Integer slotId = appointment.get().getTimeSlot().getSlotId();

            // update to 'AVAILABLE' due to being cancelled
            slotService.updateVeterinarianSlotsStatus(veterinarianId, slotId, SlotStatus.AVAILABLE);
        }

        appointment.get().setCurrentStatus(AppointmentStatus.CANCELED);

        logToStatus(appointment.get(), cancelledActor);

        appointmentRepository.save(appointment.get());

        // send email
        User customer = appointment.get().getCustomer();
        emailService.sendEmailForCancelingAppointment(appointment.get().getEmail(), "Koi fish - Thanks you", customer);
    }

    private void logToStatus(Appointment appointment, User cancelledActor) {
        Status status = new Status();

        status.setAppointment(appointment);
        status.setStatusName(appointment.getCurrentStatus().toString());
        status.setTime(LocalDateTime.now());
        status.setNote(cancelledActor.getRole().getRoleName() + " - marked CANCELLED the appointment successfully");
        status.setUser(cancelledActor);
        appointment.addStatus(status);
    }

    @Override
    public Feedback createFeedback(Integer appointmentId, Feedback feedback) throws AppointmentNotFoundException, UserNotFoundException {
        Appointment appointment = this.getAppointmentById(appointmentId);

        if (appointment.getFeedback() != null) {
            throw new FeedbackExistedException("Feedback already existed for appointment with id: " + appointmentId);
        } else {
            Feedback newFeedback = feedbackService.createFeedback(feedback, appointment);

            appointment.setFeedback(newFeedback);

            appointmentRepository.save(appointment);

            return newFeedback;
        }
    }

    @Override
    public void updateStatus(Integer appointmentId, AppointmentStatus updatedStatus) throws AppointmentUpdatedException {

        Appointment updatedAppointment = getAppointmentById(appointmentId);

        AppointmentContext appointmentContext = new AppointmentContext(updatedAppointment, appointmentStateFactory);
        appointmentContext.update(updatedAppointment);
    }


    @Override
    public Appointment createFollowUpAppointment(Integer appointmentId, Appointment newAppointment) {
        Appointment appointment = this.getAppointmentById(appointmentId);

        Appointment followUpAppointment = new Appointment();

        // Created date
        followUpAppointment.setCreatedDate(LocalDateTime.now());

        // Service
        followUpAppointment.setService(serviceService.getServiceById(4));
        BigDecimal servicePrice = serviceService.getServiceById(4).getServicePrice();

        // Address
        if (appointment.getAddress() != null) {
            followUpAppointment.setAddress(appointment.getAddress());
        }

        // Moving surcharge
        MovingSurcharge movingSurcharge = appointment.getMovingSurcharge();
        BigDecimal surchargePrice = new BigDecimal(0);
        if (movingSurcharge != null) {
            followUpAppointment.setMovingSurcharge(movingSurcharge);
            surchargePrice = movingSurcharge.getPrice();
        }

        // Slot
        Integer slotId = newAppointment.getTimeSlot().getSlotId();
        TimeSlot timeSlot = slotService.getTimeSlotById(slotId);
        followUpAppointment.setTimeSlot(timeSlot);

        // Customer
        followUpAppointment.setCustomer(appointment.getCustomer());

        // Veterinarian
        Integer veterinarianId = appointment.getVeterinarian().getUserId();
        followUpAppointment.setVeterinarian(appointment.getVeterinarian());

        // Email
        followUpAppointment.setEmail(appointment.getEmail());

        // Phone number
        followUpAppointment.setPhoneNumber(appointment.getPhoneNumber());

        // Customer name
        followUpAppointment.setCustomerName(appointment.getCustomerName());

        // description
        followUpAppointment.setDescription(newAppointment.getDescription());

        // Total price
        BigDecimal totalPrice = servicePrice.add(surchargePrice);
        followUpAppointment.setTotalPrice(totalPrice);

        // Fish
        if (appointment.getFish() != null) {
            followUpAppointment.setFish(appointment.getFish());
        }

        // Payment
        Payment payment = new Payment();
        payment.setAmount(totalPrice);
        payment.setPaymentMethod(PaymentMethod.CASH);
        payment.setStatus(PaymentStatus.NOT_PAID);

        followUpAppointment.setPayment(paymentService.createPayment(payment));

        // Current status
        followUpAppointment.setCurrentStatus(AppointmentStatus.PENDING);

        Appointment savedFollowUpAppointment = appointmentRepository.save(followUpAppointment);

        // Set follow-up appointment for main appointment
        appointment.setFollowUpAppointment(savedFollowUpAppointment);


        // Update Veterinarian_Slot status
        slotService.updateVeterinarianSlotsStatus(veterinarianId, slotId, SlotStatus.BOOKED);

        return savedFollowUpAppointment;
    }

    @Override
    public List<Appointment> getAllAppointmentsByFishId(Integer fishId) {
        List<Appointment> appointments = appointmentRepository.findAppointmentByFishId(fishId);
        if (appointments.isEmpty()) {
            throw new AppointmentNotFoundException("Appointment not found!");
        }
        // Sort by newest appointment
        appointments.sort(Comparator.comparing(Appointment::getAppointmentId).reversed());

        return appointments;
    }

    @Override
    public void unAssignedVeterinarianOnSlot(Integer veterinarianId, Integer slotId) {
        Appointment appointment = appointmentRepository.findAppointmentByVeterinarianIdAndSlotId(veterinarianId, slotId);
        if (appointment == null) throw new TimeSlotNotFoundException("TimeSlot not found with id: " + slotId);
        appointment.setVeterinarian(null);
        appointmentRepository.save(appointment);
    }


    @Override
    public Appointment getAppointmentById(Integer appointmentId) {
        Optional<Appointment> appointmentOptional = appointmentRepository.findById(appointmentId);
        if (appointmentOptional.isEmpty())
            throw new AppointmentNotFoundException("Not found Appointment with Id: " + appointmentId);
        return appointmentOptional.get();
    }

    @Override
    public MedicalReport getMedicalReportByAppointmentId(Integer appointmentId) throws AppointmentNotFoundException {
        Appointment appointment = getAppointmentById(appointmentId);
        MedicalReport medicalReport = appointment.getMedicalReport();
        if (medicalReport == null) {
            throw new MedicalReportNotFoundException("Not found Medical Report with appointment id: " + appointmentId);
        }
        return medicalReport;
    }

    @Override
    public void assignVeterinarian(Integer appointmentId, Integer veterinarianId) {
        Appointment assignedAppointment = getAppointmentById(appointmentId);
        if (assignedAppointment.getVeterinarian() == null) {
            User veterinarian = userService.getVeterinarianById(veterinarianId);

            // assign Appointment for Vet
            assignedAppointment.setVeterinarian(veterinarian);
            appointmentRepository.save(assignedAppointment);

            // update status schedule
            Integer slotId = assignedAppointment.getTimeSlot().getSlotId();
            VeterinarianSlots veterinarianSlot = veterinarianSlotsRepository.getVeterinarianSlotsById(veterinarianId, slotId);
            veterinarianSlot.setStatus(SlotStatus.BOOKED);
            veterinarianSlotsRepository.save(veterinarianSlot);
        }
    }


    private BigDecimal calculatePrice(Appointment appointment) {
        BigDecimal servicePrice = appointment.getService().getServicePrice();
        servicePrice = appointment.getMovingSurcharge() == null ? servicePrice : servicePrice.add(appointment.getMovingSurcharge().getPrice());
        servicePrice = appointment.getVoucher() == null ? servicePrice : servicePrice.subtract(appointment.getVoucher().getDiscountAmount());
        return servicePrice;
    }

    public Integer getAppointmentOwnerId(Integer appointmentId) {
        Appointment appointment = getAppointmentById(appointmentId);
        return appointment.getCustomer().getUserId();
    }

    /**
     * Appointment can update with new slotId if new slotId is available
     */
//    private boolean isAbleToUpdateAppointment(Appointment appointment, AppointmentUpdateDto appointmentUpdateDto) throws AppointmentUpdatedException {
//        LocalDateTime openingTime = appointment.getTimeSlot().getDateTimeBasedOnSlot();
//        LocalDateTime now = LocalDateTime.now();
//        LocalDateTime threeHoursFromNow = now.plusHours(3);
//
//        TimeSlot timeSlot = slotService.getTimeSlotById(appointmentUpdateDto.getSlotId());
//        List<TimeSlot> availableTimeSlot = slotService.getListAvailableTimeSlots();
//
//        return openingTime.isAfter(threeHoursFromNow)
//                && availableTimeSlot.contains(timeSlot)
//                && (appointment.getCurrentStatus().equals(AppointmentStatus.PENDING)
//                || appointment.getCurrentStatus().equals(AppointmentStatus.CONFIRMED));
//    }
    private boolean isAbleToUpdateAppointment(Appointment appointment, AppointmentUpdateDto appointmentUpdateDto) throws AppointmentUpdatedException {
        LocalDateTime openingTime = appointment.getTimeSlot().getDateTimeBasedOnSlot();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threeHoursFromNow = now.plusHours(3);

        // Fetch the time slot and the list of available time slots
        TimeSlot requestedTimeSlot = slotService.getTimeSlotById(appointmentUpdateDto.getSlotId());
        List<TimeSlot> availableTimeSlots = slotService.getListAvailableTimeSlots();

        // Check if the appointment time is at least three hours from now
        if (!openingTime.isAfter(threeHoursFromNow)) {
            throw new AppointmentUpdatedException("Appointment time must be more than three hours from now.");
        }

        // Check if the requested time slot is available
        if (!availableTimeSlots.contains(requestedTimeSlot)) {
            throw new AppointmentUpdatedException("Requested time slot is not available.");
        }

        // Check if the appointment status is either PENDING or CONFIRMED
        if (!(appointment.getCurrentStatus().equals(AppointmentStatus.PENDING) || appointment.getCurrentStatus().equals(AppointmentStatus.CONFIRMED) || appointment.getCurrentStatus().equals(AppointmentStatus.ON_GOING))) {
            throw new AppointmentUpdatedException("Appointment cannot be updated in its current status.");
        }

        return true;
    }


}
