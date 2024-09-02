package mycode.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;

import com.toshiba.mwcloud.gs.RowKey;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceTicket {
  @RowKey
  public Date createdDate;
  public String caseNumber;
  public Date closedDate;
  public String subject;
  public String status;
  public String priority;
  public double resolutionTime;
}
