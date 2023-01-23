package ua.goit.dev6.note;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import ua.goit.dev6.account.UserService;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RequiredArgsConstructor
@Controller
@RequestMapping("/notes")
public class NoteController {
    private final NoteService noteService;
    private final UserService userService;



    @GetMapping
    private ModelAndView findAll(){
        ModelAndView result = new ModelAndView("notes/notes");
        UUID authorizedUserId = userService.getAuthorizedUser().getId();
        List<NoteDTO> notes = noteService.findByUserId(authorizedUserId);
        result.addObject("notes", notes);
        return result;
    }

    @GetMapping("/create")
    private String getCreateForm() {
        return "notes/createNoteForm";
    }

    @GetMapping("/{id}")
    private ModelAndView getEditForm(@PathVariable("id") UUID id) {
        NoteDTO note = noteService.findById(id);
        if (Objects.isNull(note)){
            return new ModelAndView("error/forbidden");
        }
        UUID authorizedUserId = userService.getAuthorizedUser().getId();
        UUID ownerId = note.getUser().getId();
        if (!authorizedUserId.equals(ownerId)){
            return new ModelAndView("error/forbidden");
        }

        ModelAndView result = new ModelAndView("notes/editNoteForm");
        result.addObject("note", note);
        return result;
    }

    @PostMapping
    private ModelAndView save(@Validated @ModelAttribute("note") NoteDTO note, BindingResult bindingResult){
        if (bindingResult.hasErrors()) {
            ModelAndView model = new ModelAndView("error/noteCreateError");
            model.addObject("errors", bindingResult.getFieldErrors());
            return model;
        }
        if (Objects.nonNull(note.getId())){
            note.setId(null);
        }
        note.setUser(userService.getAuthorizedUser());
        noteService.save(note);
        return findAll();
    }
    @PutMapping("/{id}")
    private String update(@Validated @RequestBody NoteDTO note, BindingResult result, @PathVariable("id") UUID id){
        if (result.hasErrors()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Note not updated!");
        }
        UUID authorizedUserId = userService.getAuthorizedUser().getId();
        UUID ownerId = noteService.findById(id).getUser().getId();
        if (!authorizedUserId.equals(ownerId)){
            return "error/forbidden";
        }
        note.setId(id);
        note.setUser(userService.getAuthorizedUser());
        noteService.save(note);
        throw new ResponseStatusException(HttpStatus.OK, "Note Updated");
    }

    @DeleteMapping("/{id}")
    private void delete(@PathVariable("id") UUID id) {
        noteService.deleteById(id);
        throw new ResponseStatusException(HttpStatus.OK, "Note deleted");
    }

    @GetMapping("/share/{id}")
    private ModelAndView getPublicNote(@PathVariable("id") UUID id) {
        NoteDTO note = noteService.findById(id);
        if (note.getAccessType().equals(AccessType.PUBLIC)){
            return new ModelAndView("notes/publicNote").addObject("note", note);
        }
        return new ModelAndView("error/forbidden");
    }

    @ModelAttribute("note")
    private NoteDTO getDefaultProduct() {
        return new NoteDTO();
    }

}
