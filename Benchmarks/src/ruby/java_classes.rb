require 'java'

URLClassLoader = java.net.URLClassLoader
URL = java.net.URL

CLASSES_DIR = "#{Dir.pwd}/#{File.dirname __FILE__}/../../bin"
LIBS_DIR = "#{Dir.pwd}/#{File.dirname __FILE__}/../../lib"

class JClassWrapper
	def initialize(clazz)
		@clazz = clazz
	end
	
	def method_missing(sym, *args)
		jarg_types = java.lang.Class[args.size].new
		jargs = java.lang.Object[args.size].new
		
		for i in 0..(args.length - 1)
			if args[i].kind_of? String
				args[i] = java.lang.String.new args[i]
			elsif args[i].kind_of? Fixnum
				args[i] = java.lang.Integer.new args[i]
			end
			
			jarg_types[i] = args[i].java_class
			jargs[i] = args[i]
		end
		
		if sym == :new
			begin
				constructor = @clazz.getConstructor jarg_types
			rescue
				return super
			end
			
			return constructor.newInstance(jargs)
		elsif sym == :java_class
			return @clazz
		end
		
		begin
			method = @clazz.getMethod(sym.to_s, jarg_types)
		rescue
			return super
		end
		
		return method.invoke(nil, jargs) if defined? method
		
		super
	end
end

def get_class(name)
	JClassWrapper.new java.lang.Class.forName(name, true, CLASSLOADER)
end

jars = []
Dir.foreach LIBS_DIR do |fname|
	if fname =~ /\.jar$/ or fname =~ /\.war$/
		jars.push "/#{Dir.pwd}/#{File.dirname __FILE__}/#{LIBS_DIR}/#{fname}"
	end
end
jars.push "#{Dir.pwd}/#{File.dirname __FILE__}/../../../ActiveObjects/bin/"

classloader_urls = URL[1 + jars.size].new

n = -1
classloader_urls[n += 1] = URL.new "file://#{CLASSES_DIR}/"
jars.each do |jar| 
	classloader_urls[n += 1] = URL.new "file://#{jar}"
end

CLASSLOADER = URLClassLoader.new classloader_urls
