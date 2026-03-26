package com.ishijima.portfoliobackend.controller.admin;

import com.ishijima.portfoliobackend.entity.Hamster;
import com.ishijima.portfoliobackend.entity.HamsterSex;
import com.ishijima.portfoliobackend.entity.HamsterStatus;
import com.ishijima.portfoliobackend.form.HamsterForm;
import com.ishijima.portfoliobackend.service.HamsterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/hamsters")
@RequiredArgsConstructor
public class AdminHamsterController {

	private static final List<String> SPECIES_OPTIONS = List.of(
		"ゴールデンハムスター",
		"ジャンガリアンハムスター",
		"ロボロフスキーハムスター",
		"キャンベルハムスター",
		"チャイニーズハムスター",
		"シベリアンハムスター",
		"ドワーフハムスター"
	);

	private final HamsterService hamsterService;

	@ModelAttribute("hamsterStatuses")
	public HamsterStatus[] hamsterStatuses() {
		return HamsterStatus.values();
	}

	@ModelAttribute("hamsterSexes")
	public HamsterSex[] hamsterSexes() {
		return HamsterSex.values();
	}

	@ModelAttribute("speciesMasterOptions")
	public List<String> speciesMasterOptions() {
		return SPECIES_OPTIONS;
	}

	@GetMapping
	public String list(
		@RequestParam(required = false) String species,
		@RequestParam(required = false) HamsterSex sex,
		@RequestParam(required = false) HamsterStatus status,
		Model model
	) {
		List<Hamster> hamsters = hamsterService.findAll(species, sex, status);
		long availableCount = hamsters.stream().filter(hamster -> hamster.getStatus() == HamsterStatus.AVAILABLE).count();
		long negotiatingCount = hamsters.stream().filter(hamster -> hamster.getStatus() == HamsterStatus.NEGOTIATING).count();
		long adoptedCount = hamsters.stream().filter(hamster -> hamster.getStatus() == HamsterStatus.ADOPTED).count();

		model.addAttribute("hamsters", hamsters);
		model.addAttribute("speciesOptions", hamsterService.findSpeciesOptions());
		model.addAttribute("selectedSpecies", species);
		model.addAttribute("selectedSex", sex);
		model.addAttribute("selectedStatus", status);
		model.addAttribute("hamsterCount", hamsters.size());
		model.addAttribute("availableCount", availableCount);
		model.addAttribute("negotiatingCount", negotiatingCount);
		model.addAttribute("adoptedCount", adoptedCount);
		model.addAttribute("adminSection", "hamsters");
		return "admin/hamsters";
	}

	@GetMapping("/new")
	public String createForm(Model model) {
		if (!model.containsAttribute("hamsterForm")) {
			model.addAttribute("hamsterForm", new HamsterForm());
		}
		populateFormScreen(model, "新規ハムスター登録", "/admin/hamsters", true, null);
		return "admin/hamster-form";
	}

	@PostMapping
	public String create(
		@Valid @ModelAttribute("hamsterForm") HamsterForm form,
		BindingResult bindingResult,
		Model model,
		RedirectAttributes redirectAttributes
	) {
		if (bindingResult.hasErrors()) {
			populateFormScreen(model, "新規ハムスター登録", "/admin/hamsters", true, null);
			return "admin/hamster-form";
		}

		List<Hamster> createdHamsters = hamsterService.createMultiple(form);
		redirectAttributes.addFlashAttribute("hamsterMessage", createdHamsters.size() + "匹のハムスターを登録しました。");
		return "redirect:/admin/hamsters";
	}

	@GetMapping("/{id}")
	public String detail(@PathVariable Long id, Model model) {
		model.addAttribute("hamster", hamsterService.findById(id));
		model.addAttribute("adminSection", "hamsters");
		return "admin/hamster-detail";
	}

	@GetMapping("/{id}/edit")
	public String editForm(@PathVariable Long id, Model model) {
		Hamster hamster = hamsterService.findById(id);
		if (!model.containsAttribute("hamsterForm")) {
			HamsterForm form = new HamsterForm();
			form.setSpecies(hamster.getSpecies());
			form.setSex(hamster.getSex());
			form.setBirthDate(hamster.getBirthDate());
			form.setArrivalDate(hamster.getArrivalDate());
			form.setStatus(hamster.getStatus());
			form.setQuantity(1);
			form.setNotes(hamster.getNotes());
			model.addAttribute("hamsterForm", form);
		}

		populateFormScreen(model, "ハムスター情報の編集", "/admin/hamsters/" + id, false, hamster);
		return "admin/hamster-form";
	}

	@PostMapping("/{id}")
	public String update(
		@PathVariable Long id,
		@Valid @ModelAttribute("hamsterForm") HamsterForm form,
		BindingResult bindingResult,
		Model model,
		RedirectAttributes redirectAttributes
	) {
		Hamster hamster = hamsterService.findById(id);
		if (bindingResult.hasErrors()) {
			populateFormScreen(model, "ハムスター情報の編集", "/admin/hamsters/" + id, false, hamster);
			return "admin/hamster-form";
		}

		hamsterService.update(id, form);
		redirectAttributes.addFlashAttribute("hamsterMessage", "ハムスター情報を更新しました。");
		return "redirect:/admin/hamsters/" + id;
	}

	@PostMapping("/{id}/delete")
	public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		hamsterService.deleteById(id);
		redirectAttributes.addFlashAttribute("hamsterMessage", "ハムスター情報を削除しました。");
		return "redirect:/admin/hamsters";
	}

	private void populateFormScreen(Model model, String pageTitle, String submitPath, boolean createMode, Hamster hamster) {
		model.addAttribute("pageTitle", pageTitle);
		model.addAttribute("submitPath", submitPath);
		model.addAttribute("createMode", createMode);
		model.addAttribute("hamster", hamster);
		model.addAttribute("adminSection", "hamsters");
	}
}
