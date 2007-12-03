/*
 * Copyright 2007 Daniel Spiewak
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 * 
 *	    http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.ao;

import test.schema.Distribution;

/**
 * @author Daniel Spiewak
 */
public class DataStruct {
	public int personID;
	public long companyID;
	
	public int[] penIDs;
	public int[] defenceIDs;
	public int[] suitIDs;
	
	public long[] coolCompanyIDs;
	
	public int postID;
	public int photoID;
	
	public int[] postCommentIDs;
	public int[] photoCommentIDs;
	
	public int[] bookIDs;
	public int[] magazineIDs;
	
	public int[][] bookAuthorIDs;
	public int[][] magazineAuthorIDs;
	
	public int[][] bookDistributionIDs;
	public Class<? extends Distribution>[][] bookDistributionTypes;
	
	public int[][] magazineDistributionIDs;
	public Class<? extends Distribution>[][] magazineDistributionTypes;
}
