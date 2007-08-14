workplace = create_workplace
workplace.office_name = 'Hell on Earth'
workplace.coffee_quality = -10
workplace.save

profession1 = create_profession
profession1.name = 'Painting'
profession1.save

profession2 = create_profession
profession2.name = 'Sculpting'
profession2.save

profession3 = create_profession
profession3.name = 'Programming'
profession3.save

person = create_person
person.first_name = 'Daniel'
person.last_name = 'Spiewak'
person.age = 19
person.alive = true
person.workplace = workplace
person.save

professional = create_professional
professional.person = person
professional.profession = profession3
professional.save

person = create_person
person.first_name = 'Jason'
person.last_name = 'Dvorak'
person.age = 29
person.alive = false
person.workplace = workplace
person.save

professional = create_professional
professional.person = person
professional.profession = profession1
professional.save

person = create_person
person.first_name = 'Douglas'
person.last_name = 'Carson'
person.age = 12
person.alive = true
person.workplace = workplace
person.save

professional = create_professional
professional.person = person
professional.profession = profession1
professional.save

person = create_person
person.first_name = 'Seamous'
person.last_name = 'Tailoy'
person.age = 48
person.alive = false
person.save

professional = create_professional
professional.person = person
professional.profession = profession2
professional.save

person = create_person
person.first_name = 'James'
person.last_name = 'Dinglham'
person.age = 31
person.alive = true
person.workplace = workplace
person.save

professional = create_professional
professional.person = person
professional.profession = profession1
professional.save

person = create_person
person.first_name = 'Karen'
person.last_name = 'Haevaen'
person.age = 29
person.alive = false
person.workplace = workplace
person.save

professional = create_professional
professional.person = person
professional.profession = profession2
professional.save

person = create_person
person.first_name = 'Tyler'
person.last_name = 'Flickt'
person.age = 2
person.alive = false
person.workplace = workplace
person.save

professional = create_professional
professional.person = person
professional.profession = profession2
professional.save
