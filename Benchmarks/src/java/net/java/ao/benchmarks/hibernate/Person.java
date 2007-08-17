/*
 * Created on Aug 14, 2007
 */
package net.java.ao.benchmarks.hibernate;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Daniel Spiewak
 */
public class Person {
	private Long id;

	private String firstName, lastName, bio;
	private int age;
	private boolean alive;
	
	private Workplace workplace;
	private Set professions = new HashSet();
	
	public Person() {
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getBio() {
		return bio;
	}

	public void setBio(String bio) {
		this.bio = bio;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public boolean isAlive() {
		return alive;
	}

	public void setAlive(boolean alive) {
		this.alive = alive;
	}

	public Workplace getWorkplace() {
		return workplace;
	}

	public void setWorkplace(Workplace workplace) {
		this.workplace = workplace;
	}

	public Set getProfessions() {
		return professions;
	}

	public void setProfessions(Set professions) {
		this.professions = professions;
	}
}
