package com.blazer.ddl.tool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blazer.db.TableColumns;
import com.blazer.pipeline.PipeLineTask;


public class OraTableDataLineExport  implements PipeLineTask{
	private static final Logger _logger = LoggerFactory.getLogger(OraTableDataLineExport.class);
	FileOutputStream out;
	String url;
	String user;
	String pass;
	String driverClass;
	DataSource dataSource;
	String owner;
	
	String configFilePath;
	String exportFilePath;
	Connection conn;
	String terminatedString;

	
	@Override
	public int execute() throws Exception {
		//String filePath="E:/MHSHI/ora.txt";
		//String oraExport="E:/MHSHI/oraExport.txt";
		conn = dataSource.getConnection();
		File exportFile=new File(exportFilePath);
		if(exportFile.exists()){
			exportFile.delete();
		}
		exportFile.createNewFile();
		out=new FileOutputStream(exportFile);
		out.write(("-- --FROM SERVER " + this.url + "\n").getBytes());
		out.write(("-- --FROM USER " + this.user + "\n").getBytes());
		out.write(("-- --TO FILE " + this.exportFilePath + "\n").getBytes());
		readExportData(configFilePath);
		
		out.flush();
		out.close();
		conn.close();
		return 0;
	}
	
	public void  readTable(String tableName) throws Exception{
		//out.write("--Create Table \r\n".getBytes());
		Statement stmt=conn.createStatement();
		/*String sql="select t.owner,t.table_name,c.COMMENTS from sys.all_all_tables t,sys.all_tab_comments c "+
					" where t.table_name=c.TABLE_NAME "+
					" and t.table_name='"+tableName+"' and t.owner ='"+owner+"' "+
					" order by t.owner,t.table_name";*/
		String sql="select t.owner,t.table_name,c.COMMENTS as TABLECOMMENTS,col.COLUMN_NAME,"+
			" col.DATA_TYPE,col.DATA_LENGTH,col.DATA_SCALE,col.DATA_PRECISION,colc.COMMENTS AS COLUMNCOMMENTS ,col.NULLABLE,col.DATA_DEFAULT"+
			" from sys.all_all_tables t,sys.all_tab_comments c,sys.all_tab_columns col,sys.all_col_comments colc "+
			" where t.table_name=c.TABLE_NAME "+
			" and col.TABLE_NAME=t.table_name "+
			" and colc.TABLE_NAME=t.table_name "+
			" and col.COLUMN_NAME=colc.COLUMN_NAME "+
			" and t.owner ='"+owner+"' "+
			" and c.owner ='"+owner+"' "+
			" and col.owner ='"+owner+"' "+
			" and colc.owner ='"+owner+"' "+
			" and upper(t.table_name)='"+tableName.toUpperCase()+"' "+
			" order by t.owner,t.table_name,col.COLUMN_ID";
		
		_logger.info(sql);
		ArrayList<TableColumns> tcArray=new ArrayList<TableColumns>();
		ResultSet rs=stmt.executeQuery(sql);
		String selectSql="SELECT \n\r";
		while(rs.next()){
			_logger.info("COLUMN_NAME : "+rs.getString("COLUMN_NAME"));
			TableColumns tc=new TableColumns();
			tc.setOwner(rs.getString("owner"));
			tc.setTableName(rs.getString("table_name"));
			tc.setTableComments(rs.getString("TABLECOMMENTS"));
			tc.setColumnName(rs.getString("COLUMN_NAME"));
			tc.setColumnComments(rs.getString("COLUMNCOMMENTS"));
			tc.setDataType(rs.getString("DATA_TYPE"));
			tc.setDataLength(rs.getInt("DATA_LENGTH"));
			tc.setDataScale(rs.getInt("DATA_SCALE"));
			tc.setDataPrecision(rs.getInt("DATA_PRECISION"));
			tc.setNullAble(rs.getString("NULLABLE"));
			tc.setDefaultValue(rs.getString("DATA_DEFAULT"));
			//DATA_DEFAULT
			//col.NULLABLE
			
			
			tcArray.add(tc);
		}
		int count=1;
		for(TableColumns tc : tcArray){
			if(count!=tcArray.size()){
				//selectSql+=(String.format("%-20s", tc.getColumnName())+"||'|+|'||");
				selectSql+=("\t"+String.format("%-20s", tc.getColumnName())+",\r\n");
			}else{
				selectSql+=("\t"+String.format("%-20s", tc.getColumnName())+"");
			}
			count++;
		}

		
		selectSql+="\r\n"+"FROM "+tableName.toUpperCase();
		
		out.write((selectSql+"\r\n").getBytes());
		rs.close();
		stmt.close();
		
	}
	
	public void  readExportData(String filePath) throws Exception{
		File txtFile=new File(filePath);
		if(txtFile.exists()){
			InputStreamReader read=new InputStreamReader(new FileInputStream(filePath));
			BufferedReader bReader=new BufferedReader(read);
			String lineText;
			int sourceCount=0;
			while((lineText=bReader.readLine())!=null){
				if(lineText.startsWith("#")){
					//���?
				}else if(lineText.startsWith("--")){
					
				}else if(lineText.startsWith("++")){
					
				}else{
					_logger.info("-- --No." + sourceCount + " , Table : " + lineText.toUpperCase() + "\r\n");
					sourceCount++;
					out.write(("-- --No." + sourceCount + " , Table : " + lineText.toUpperCase() + "\r\n").getBytes());
					out.write(("(\r\n\r\n").getBytes());
					
					//out.write(("--Script Table "+lineText.toUpperCase()+" Start .\r\n").getBytes());
					readTable(lineText);
					out.write(("\r\n)\r\n").getBytes());
					
					//out.write(("--Script Table "+lineText.toUpperCase()+" End .\r\n\r\n").getBytes());
				}
			}
			bReader.close();
		}else{
			_logger.info("");
		}
	}

	public DataSource getDataSource() {
		return dataSource;
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public String getConfigFilePath() {
		return configFilePath;
	}

	public void setConfigFilePath(String configFilePath) {
		this.configFilePath = configFilePath;
	}

	public String getExportFilePath() {
		return exportFilePath;
	}

	public void setExportFilePath(String exportFilePath) {
		this.exportFilePath = exportFilePath;
	}

	public String getTerminatedString() {
		return terminatedString;
	}

	public void setTerminatedString(String terminatedString) {
		this.terminatedString = terminatedString;
	}


	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}
	
}
