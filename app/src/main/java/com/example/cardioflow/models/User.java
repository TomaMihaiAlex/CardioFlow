package com.example.cardioflow.models;

public class User {
    private String id;
    private String email;
    private String password; // în realitate hash, acum plain
    private String role;     // "pacient" sau "medic"
    private String firstName;
    private String lastName;
    private String doctorId;  // pentru pacient: id-ul medicului asociat

    // Constructor gol necesar pentru Gson
    public User() {}

    // Constructor cu parametri
    public User(String id, String email, String password, String role, String firstName, String lastName, String doctorId) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.role = role;
        this.firstName = firstName;
        this.lastName = lastName;
        this.doctorId = doctorId;
    }

    // Gettere și settere
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }
}