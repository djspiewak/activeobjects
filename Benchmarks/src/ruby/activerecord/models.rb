require 'rubygems'
require 'active_record'

class Person < ActiveRecord::Base
	belongs_to :workplace
	
	has_many :professionals
	has_many :professions, :through => :professionals
end

class Profession < ActiveRecord::Base
end

class Professional < ActiveRecord::Base
	belongs_to :person
	belongs_to :profession
end

class Workplace < ActiveRecord::Base
	has_many :people
	
	def office_name
		"Office: #{super}"
	end
end

def create_workplace
	Workplace.create
end

def create_profession
	Profession.create
end

def create_person
	Person.create
end

def create_professional
	Professional.create
end

ActiveRecord::Base.establish_connection(  
	:adapter  => 'mysql',   
	:database => 'ao_test2',   
	:username => 'root',   
	:password => 'mysqlroot',   
	:host     => 'localhost')
	
ActiveRecord::Base.logger = Logger.new(STDERR)
ActiveRecord::Base.colorize_logging = false