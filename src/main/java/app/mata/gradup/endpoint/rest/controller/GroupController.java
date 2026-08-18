package app.mata.gradup.endpoint.rest.controller;

import app.mata.gradup.endpoint.rest.model.GroupCreateRequest;
import app.mata.gradup.endpoint.rest.model.GroupResponse;
import app.mata.gradup.service.GroupService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class GroupController {

  private final GroupService groupService;

  @GetMapping("/groups")
  public List<GroupResponse> listGroups(
      @RequestParam(required = false) UUID cohortId, @RequestParam(required = false) UUID trackId) {
    return groupService.listGroups(cohortId, trackId);
  }

  @PostMapping("/groups")
  @ResponseStatus(HttpStatus.CREATED)
  public GroupResponse createGroup(@RequestBody @Valid GroupCreateRequest request) {
    return groupService.createGroup(request);
  }
}
