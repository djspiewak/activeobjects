require 'rubygems'
require 'active_record'
require 'models'
require '../tests'

class ARTest < TestClass
	def test_queries
		start_timer
		
		children = Person.find :all, :conditions => 'age < 18'
		adults = Person.find :all, :conditions => 'age >= 18'
		professions = Profession.find :all
		professionals = Professional.find :all
		people = Person.find :all
		
#		puts "Queries test: #{stop_timer} ms"
		
		people
	end
end

TEST_CLASS = ARTest
run_tests