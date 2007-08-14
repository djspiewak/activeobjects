class TestClass
	def test_persisting(people)
		new_names = ['Daniel', 'Chris', 'Joseph', 'Renee', 'Bethany', 'Grace', 'Karen', 'Larry', 'Moya']
		last_names = ['Smith', 'Donovich', 'Quieones', 'Felger', 'Gere', 'Covis', 'Dawes']
		
		start_timer
		first_i = -1
		last_i = -1
		people.each do |person|
			person.first_name = new_names[first_i += 1]
			person.last_name = last_names[last_i += 1]
			person.save
			
			first_i = -1 if first_i == new_names.size
			last_i = -1 if last_i == last_names.size
		end
		
		iter = 15
		people.each do |person|
			person.age = iter += 1
			person.alive = (iter % 2 == 0)		# only people with even ages are still living

			person.bio = <<TERM
This is the story of two mice.  Well, actually it's the story of more than 
two mice, but we only have time for the shortened version.  They (the mice)
were on this road one day, looking for upturned clods of grass - for you see,
this is what mice do - and they came across a peddler, peddling his wares.
After the usual confusion between 'ware', 'where' and 'were' (leading to some
dicy moments involving a silver bullet and frantic references to the impending
lunar cycle, the mice managed to extract a piece of useful information out of the
peddler's ramblings.  However, the remainder of this story, and the usefulness
of the peddlers account will have to wait for the SQL, which I am afraid is
going to be very late in arrival.
TERM
			person.save
		end
		
#		puts "Persistence test: #{stop_timer} ms"
	end
	
	def test_retrieval(people)
		start_timer
		
		people.each do |person|
			fname = person.first_name
			lname = person.last_name
			age = person.age
			alive = person.alive?
			bio = person.bio
		end

#		puts "Retrieval test: #{stop_timer} ms"		
	end
	
	def test_relations(people)
		start_timer
		
		people.each do |person|
			person.professions.each do |profession|
				pro_name = profession.name
			end
			
			person.workplace.people.each do |sub_person|
				sub_fname = sub_person.first_name
				sub_lname = sub_person.last_name
			end
		end

#		puts "Relations test: #{stop_timer} ms"	
	end
	
	def test_queries
		# complex queries to search for data
	end
	
	def start_timer
		@time = Time.now.to_f
	end
	
	def stop_timer
		((Time.now.to_f - @time) * 1000).to_i
	end
end

def run_tests
	tester = TEST_CLASS.new
	
	people = tester.test_queries
	tester.test_retrieval people
	tester.test_persisting people
	tester.test_relations people
end
